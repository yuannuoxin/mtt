package com.mtd.tt.service;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.model.resp.HideBlockResp;
import com.mtd.tt.util.HideMarkdown;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 隐藏内容服务：解析校验、按读者能力脱敏、积分解锁、密码解锁。
 * 表结构与 Go 版共用，Cookie 名也保持一致，切换后端后已解锁状态仍然有效。
 */
@Slf4j
@Service
public class HideContentService {

    /** 游客密码解锁 Cookie，与 Go 版一致 */
    public static final String GUEST_COOKIE = "mtd_hide_guest";
    public static final String GUEST_ATTR = "mtd_hide_guest_token";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS post_content_unlocks
                    (
                        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        post_id    BIGINT,
                        user_id    BIGINT,
                        points     BIGINT,
                        created_at TIMESTAMPTZ DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_post_user_unlock
                        ON post_content_unlocks (post_id, user_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS post_password_unlocks
                    (
                        id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        post_id     BIGINT,
                        user_id     BIGINT,
                        guest_token TEXT,
                        block_index INTEGER,
                        created_at  TIMESTAMPTZ DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_post_password_unlock
                        ON post_password_unlocks (post_id, user_id, guest_token)
                    """);
            log.info("隐藏内容解锁表初始化完成");
        } catch (Exception e) {
            log.error("隐藏内容解锁表初始化失败: {}", e.getMessage(), e);
        }
    }

    /** 发布前校验隐藏块语法；无隐藏内容直接通过 */
    public void validateForCreate(String content) {
        if (!HideMarkdown.hasHideIntent(content)) {
            return;
        }
        try {
            HideMarkdown.validate(content);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
        }
    }

    /**
     * 按当前读者能力脱敏正文，并返回每块访问状态。
     * postId/authorId 为正文所属帖子；viewerId 为当前登录用户（游客为 null）。
     */
    public HideMarkdown.Sanitized sanitize(String content, Long postId, Long authorId, Long viewerId) {
        if (!HideMarkdown.hasHideIntent(content)) {
            return new HideMarkdown.Sanitized(content, Collections.emptyList());
        }
        HideMarkdown.ViewerCaps caps;
        try {
            boolean bypass = viewerId != null
                    && (viewerId.equals(authorId) || isAdmin(viewerId));
            boolean replied = viewerId != null && hasReplied(viewerId, postId);
            boolean paid = viewerId != null && hasPaid(viewerId, postId);
            Set<Integer> passwordUnlocked = passwordUnlocked(postId, viewerId, currentGuestToken());
            caps = new HideMarkdown.ViewerCaps(bypass, viewerId != null, replied, paid, passwordUnlocked);
            return HideMarkdown.sanitize(content, caps);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
        }
    }

    public List<HideBlockResp> toRespList(List<HideMarkdown.Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        List<HideBlockResp> list = new ArrayList<>(statuses.size());
        for (HideMarkdown.Status s : statuses) {
            HideBlockResp r = new HideBlockResp();
            r.setIndex(s.index);
            r.setKind(s.kind);
            r.setPoints(s.points);
            r.setUnlocked(s.unlocked);
            list.add(r);
        }
        return list;
    }

    /** 当前请求的游客 Cookie（优先读控制器刚签发时塞入的 request attribute） */
    public String currentGuestToken() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        Object attr = request.getAttribute(GUEST_ATTR);
        if (attr instanceof String token && !token.isBlank()) {
            return token;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (GUEST_COOKIE.equals(cookie.getName()) && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 密码解锁前确保游客有一个随机令牌；返回可用于本次脱敏的令牌。
     * 已登录用户不需要游客 Cookie，返回已有值或 null。
     */
    public String ensureGuestToken(HttpServletRequest request, HttpServletResponse response, boolean guest) {
        String existing = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (GUEST_COOKIE.equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    existing = cookie.getValue();
                    break;
                }
            }
        }
        if (existing != null) {
            request.setAttribute(GUEST_ATTR, existing);
            return existing;
        }
        if (!guest) {
            return null;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        ResponseCookie cookie = ResponseCookie.from(GUEST_COOKIE, token)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(Duration.ofDays(30))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        request.setAttribute(GUEST_ATTR, token);
        return token;
    }

    public int requiredPoints(String content) {
        try {
            return HideMarkdown.sumPoints(HideMarkdown.parse(content));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
        }
    }

    public List<Integer> matchPassword(String content, String password) {
        try {
            return HideMarkdown.matchPassword(content, password);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
        }
    }

    /**
     * 积分解锁：一次支付解锁本帖全部积分隐藏块。
     * 作者/管理员免费；同一用户只扣一次，读者扣分、作者加分、双方流水同时落库。
     */
    @Transactional(rollbackFor = Exception.class)
    public long unlockByPoints(Long postId, Long userId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_id, content, article_title FROM posts WHERE id = ? AND deleted_at IS NULL",
                postId);
        if (rows.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "帖子不存在");
        }
        Long authorId = asLong(rows.get(0).get("user_id"));
        String content = asString(rows.get(0).get("content"));
        String title = asString(rows.get(0).get("article_title"));
        long need = requiredPoints(content);
        if (need <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "本帖没有积分隐藏内容");
        }
        if (authorId != null && authorId.equals(userId)) {
            return 0;
        }
        if (isAdmin(userId)) {
            return 0;
        }
        if (hasPaid(userId, postId)) {
            return 0;
        }

        int updated = jdbcTemplate.update(
                "UPDATE users SET points = points - ? WHERE id = ? AND points >= ?",
                need, userId, need);
        if (updated == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "积分不足，无法解锁");
        }
        long buyerBalance = balanceOf(userId);
        insertPointRecord(userId, "unlock_post", -need, buyerBalance, "解锁隐藏内容：" + safeTitle(title));

        if (authorId != null && authorId > 0) {
            jdbcTemplate.update("UPDATE users SET points = points + ? WHERE id = ?", need, authorId);
            long authorBalance = balanceOf(authorId);
            insertPointRecord(authorId, "unlock_post_earn", need, authorBalance,
                    "隐藏内容解锁收益：" + safeTitle(title));
        }

        jdbcTemplate.update(
                "INSERT INTO post_content_unlocks (post_id, user_id, points, created_at) VALUES (?, ?, ?, ?)",
                postId, userId, need, OffsetDateTime.now());
        return need;
    }

    /** 记录密码解锁的块下标；登录用户按 user_id，游客按 guest_token */
    @Transactional(rollbackFor = Exception.class)
    public void savePasswordUnlocks(Long postId, Long userId, String guestToken, List<Integer> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return;
        }
        long storedUserId = userId == null ? 0L : userId;
        for (Integer index : indexes) {
            long exists;
            if (userId == null) {
                Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM post_password_unlocks
                        WHERE post_id = ? AND user_id = 0 AND guest_token = ? AND block_index = ?
                        """, Long.class, postId, guestToken, index);
                exists = count == null ? 0 : count;
            } else {
                Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM post_password_unlocks
                        WHERE post_id = ? AND user_id = ? AND block_index = ?
                        """, Long.class, postId, userId, index);
                exists = count == null ? 0 : count;
            }
            if (exists > 0) {
                continue;
            }
            jdbcTemplate.update("""
                    INSERT INTO post_password_unlocks (post_id, user_id, guest_token, block_index, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, postId, storedUserId, userId == null ? guestToken : null, index, OffsetDateTime.now());
        }
    }

    private boolean hasPaid(Long userId, Long postId) {
        if (userId == null) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM post_content_unlocks WHERE post_id = ? AND user_id = ?",
                Long.class, postId, userId);
        return count != null && count > 0;
    }

    private boolean hasReplied(Long userId, Long postId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM posts
                WHERE user_id = ? AND type = 'reply' AND referenced_post_id = ? AND deleted_at IS NULL
                """, Long.class, userId, postId);
        return count != null && count > 0;
    }

    private Set<Integer> passwordUnlocked(Long postId, Long userId, String guestToken) {
        List<Integer> indexes;
        if (userId != null) {
            indexes = jdbcTemplate.queryForList("""
                    SELECT block_index FROM post_password_unlocks
                    WHERE post_id = ? AND user_id = ?
                    """, Integer.class, postId, userId);
        } else if (guestToken != null && !guestToken.isBlank()) {
            indexes = jdbcTemplate.queryForList("""
                    SELECT block_index FROM post_password_unlocks
                    WHERE post_id = ? AND user_id = 0 AND guest_token = ?
                    """, Integer.class, postId, guestToken);
        } else {
            indexes = Collections.emptyList();
        }
        return indexes == null ? Collections.emptySet() : new HashSet<>(indexes);
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        Boolean admin = jdbcTemplate.queryForObject(
                "SELECT admin FROM users WHERE id = ?", Boolean.class, userId);
        return Boolean.TRUE.equals(admin);
    }

    private long balanceOf(Long userId) {
        Long value = jdbcTemplate.queryForObject("SELECT points FROM users WHERE id = ?", Long.class, userId);
        return value == null ? 0L : value;
    }

    private void insertPointRecord(Long userId, String type, long points, long balance, String remark) {
        jdbcTemplate.update("""
                INSERT INTO point_records (user_id, type, points, balance, remark, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, type, points, balance, remark, OffsetDateTime.now());
    }

    private static String safeTitle(String title) {
        return title == null || title.isBlank() ? "帖子" : title;
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }
}
