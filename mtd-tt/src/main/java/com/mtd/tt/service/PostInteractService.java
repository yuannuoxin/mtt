package com.mtd.tt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.enums.PostKindEnum;
import com.mtd.tt.enums.PostTypeEnum;
import com.mtd.tt.enums.PointTypeEnum;
import com.mtd.tt.mapper.PostMapper;
import com.mtd.tt.model.entity.PostEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子互动：讨论、问答、投票、悬赏、抽奖。
 * 元数据存在 posts.interact_meta(JSON)，投票明细存在 post_poll_votes。
 */
@Service
public class PostInteractService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("ALTER TABLE posts ADD COLUMN IF NOT EXISTS kind VARCHAR(16) NOT NULL DEFAULT 'discussion'");
        jdbcTemplate.execute("ALTER TABLE posts ADD COLUMN IF NOT EXISTS interact_meta TEXT");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS post_poll_votes (" +
                "id BIGSERIAL PRIMARY KEY," +
                "post_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "option_index INT NOT NULL," +
                "created_at TIMESTAMPTZ NOT NULL DEFAULT now()," +
                "UNIQUE(post_id, user_id, option_index))");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_post_poll_votes_post ON post_poll_votes(post_id)");
    }

    public static String kindOf(PostEntity post) {
        String kind = post == null ? null : post.getKind();
        if (kind == null || kind.isBlank()) {
            return PostKindEnum.DISCUSSION.getCode();
        }
        return kind;
    }

    public boolean supports(String kind) {
        return PostKindEnum.of(kind) != null;
    }

    /**
     * 校验并规范化互动配置。返回的 kind 与 metaJson 直接写入帖子。
     */
    public Normalized normalize(String rawKind, Map<String, Object> raw) {
        String kind = rawKind == null || rawKind.isBlank() ? PostKindEnum.DISCUSSION.getCode() : rawKind;
        PostKindEnum kindEnum = PostKindEnum.of(kind);
        if (kindEnum == null) {
            throw bad("不支持的互动类型：" + kind);
        }
        Map<String, Object> meta = raw == null ? new LinkedHashMap<>() : new LinkedHashMap<>(raw);
        switch (kindEnum) {
            case DISCUSSION:
                meta.clear();
                break;
            case QUESTION:
                meta.clear();
                meta.put("solved", false);
                meta.put("acceptedCommentId", null);
                meta.put("solvedAt", null);
                break;
            case POLL:
                normalizePoll(meta);
                break;
            case BOUNTY:
                normalizeBounty(meta);
                break;
            case LOTTERY:
                normalizeLottery(meta);
                break;
            default:
                break;
        }
        try {
            return new Normalized(kind, meta.isEmpty() ? null : objectMapper.writeValueAsString(meta));
        } catch (Exception e) {
            throw bad("互动配置序列化失败");
        }
    }

    private void normalizePoll(Map<String, Object> meta) {
        Object rawOptions = meta.get("options");
        List<String> options = new ArrayList<>();
        if (rawOptions instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String s = String.valueOf(o).trim();
                if (s.isEmpty()) {
                    continue;
                }
                if (s.length() > 200) {
                    throw bad("投票选项最多 200 字");
                }
                if (options.contains(s)) {
                    throw bad("投票选项不能重复");
                }
                options.add(s);
            }
        }
        if (options.size() < 2 || options.size() > 10) {
            throw bad("投票选项需要 2-10 个");
        }
        boolean multi = bool(meta.get("multi"), false);
        boolean anonymous = bool(meta.get("anonymous"), false);
        String endsAt = str(meta.get("endsAt"));
        meta.clear();
        meta.put("options", options);
        meta.put("multi", multi);
        meta.put("anonymous", anonymous);
        meta.put("endsAt", endsAt);
        meta.put("closed", false);
        meta.put("closedAt", null);
    }

    private void normalizeBounty(Map<String, Object> meta) {
        int points = intVal(meta.get("points"), -1);
        if (points < 1 || points > 100000) {
            throw bad("悬赏积分需在 1-100000 之间");
        }
        String endsAt = str(meta.get("endsAt"));
        meta.clear();
        meta.put("points", points);
        meta.put("endsAt", endsAt);
        meta.put("solved", false);
        meta.put("acceptedCommentId", null);
        meta.put("acceptedAt", null);
        meta.put("escrowed", true);
        meta.put("refunded", false);
        meta.put("awarded", false);
    }

    private void normalizeLottery(Map<String, Object> meta) {
        Object rawPrizes = meta.get("prizes");
        List<Map<String, Object>> prizes = new ArrayList<>();
        int slots = 0;
        if (rawPrizes instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> pm)) {
                    continue;
                }
                String name = str(pm.get("name"));
                int count = intVal(pm.get("count"), 0);
                if (name == null || name.isBlank()) {
                    throw bad("奖品名称不能为空");
                }
                if (name.length() > 80) {
                    throw bad("奖品名称最多 80 字");
                }
                if (count < 1 || count > 100) {
                    throw bad("单个奖品名额需在 1-100 之间");
                }
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("name", name.trim());
                p.put("count", count);
                prizes.add(p);
                slots += count;
            }
        }
        if (prizes.isEmpty() || prizes.size() > 10) {
            throw bad("抽奖奖品需要 1-10 种");
        }
        if (slots < 1 || slots > 100) {
            throw bad("抽奖总名额需在 1-100 之间");
        }
        String endsAt = str(meta.get("endsAt"));
        int expireDays = intVal(meta.get("expireDays"), 7);
        if (expireDays < 1 || expireDays > 30) {
            expireDays = 7;
        }
        meta.clear();
        meta.put("prizes", prizes);
        meta.put("slots", slots);
        meta.put("endsAt", endsAt);
        meta.put("expireDays", expireDays);
        meta.put("drawn", false);
        meta.put("winnerIds", new ArrayList<>());
        meta.put("closed", false);
        meta.put("drawnAt", null);
    }

    /** 悬赏积分从楼主余额转入托管；余额不足时抛业务异常，由发帖事务整体回滚 */
    @Transactional(rollbackFor = Exception.class)
    public void escrowBounty(Long userId, PostEntity post) {
        if (!PostKindEnum.BOUNTY.getCode().equals(kindOf(post))) {
            return;
        }
        Map<String, Object> meta = parseMeta(post);
        int points = intVal(meta.get("points"), 0);
        if (points <= 0) {
            return;
        }
        int updated = jdbcTemplate.update(
                "UPDATE users SET points = points - ? WHERE id = ? AND points >= ?", points, userId, points);
        if (updated == 0) {
            throw bad("积分不足，无法发起悬赏");
        }
        Long balance = jdbcTemplate.queryForObject("SELECT points FROM users WHERE id = ?", Long.class, userId);
        jdbcTemplate.update(
                "INSERT INTO point_records(user_id, type, points, balance, remark, created_at) VALUES (?,?,?,?,?,now())",
                userId, PointTypeEnum.BOUNTY_ESCROW.getCode(), -points, balance, "发起悬赏托管 " + points + " 分");
    }

    /** 删除帖子时退还未结算悬赏 */
    @Transactional(rollbackFor = Exception.class)
    public void refundBounty(PostEntity post) {
        if (!PostKindEnum.BOUNTY.getCode().equals(kindOf(post))) {
            return;
        }
        Map<String, Object> meta = parseMeta(post);
        if (!bool(meta.get("escrowed"), false) || bool(meta.get("refunded"), false) || bool(meta.get("awarded"), false)) {
            return;
        }
        int points = intVal(meta.get("points"), 0);
        if (points <= 0) {
            return;
        }
        jdbcTemplate.update("UPDATE users SET points = points + ? WHERE id = ?", points, post.getUserId());
        Long balance = jdbcTemplate.queryForObject("SELECT points FROM users WHERE id = ?", Long.class, post.getUserId());
        jdbcTemplate.update(
                "INSERT INTO point_records(user_id, type, points, balance, remark, created_at) VALUES (?,?,?,?,?,now())",
                post.getUserId(), PointTypeEnum.BOUNTY_REFUND.getCode(), points, balance, "悬赏未结算退还 " + points + " 分");
        meta.put("escrowed", false);
        meta.put("refunded", true);
        saveMeta(post, meta);
    }

    public Map<String, Object> buildView(PostEntity post, Long viewerId) {
        String kind = kindOf(post);
        Map<String, Object> meta = parseMeta(post);
        Map<String, Object> view = new LinkedHashMap<>(meta);
        view.put("kind", kind);
        boolean isAuthor = viewerId != null && viewerId.equals(post.getUserId());
        boolean admin = viewerId != null && isAdmin(viewerId);
        switch (PostKindEnum.of(kind)) {
            case POLL -> fillPollView(post, view, meta, viewerId, isAuthor, admin);
            case QUESTION -> {
                Long accepted = longVal(meta.get("acceptedCommentId"));
                view.put("acceptedReplyId", accepted);
                view.put("solved", bool(meta.get("solved"), false));
                view.put("canAccept", (isAuthor || admin) && accepted == null);
            }
            case BOUNTY -> {
                Long accepted = longVal(meta.get("acceptedCommentId"));
                view.put("acceptedReplyId", accepted);
                view.put("solved", accepted != null);
                view.put("canAccept", (isAuthor || admin) && accepted == null && bool(meta.get("escrowed"), false));
            }
            case LOTTERY -> fillLotteryView(post, view, meta, viewerId, isAuthor, admin);
            default -> {
            }
        }
        return view;
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized Map<String, Object> vote(Long userId, Long postId, List<Integer> optionIndexes) {
        requireLogin(userId);
        PostEntity post = requirePost(postId);
        if (!PostKindEnum.POLL.getCode().equals(kindOf(post))) {
            throw bad("该帖子不是投票");
        }
        Map<String, Object> meta = parseMeta(post);
        if (bool(meta.get("closed"), false) || expired(meta.get("endsAt"))) {
            throw bad("投票已结束");
        }
        List<?> options = meta.get("options") instanceof List<?> l ? l : List.of();
        if (optionIndexes == null || optionIndexes.isEmpty()) {
            throw bad("请选择投票选项");
        }
        List<Integer> picked = optionIndexes.stream().distinct().collect(Collectors.toList());
        boolean multi = bool(meta.get("multi"), false);
        if (!multi && picked.size() != 1) {
            throw bad("该投票只能选 1 项");
        }
        for (Integer idx : picked) {
            if (idx == null || idx < 0 || idx >= options.size()) {
                throw bad("投票选项不存在");
            }
        }
        // 整组覆盖：先清空该用户在这条投票下的旧票，再写入本次选择。
        // 多选修改时既能增加选项，也能取消已选项。
        jdbcTemplate.update("DELETE FROM post_poll_votes WHERE post_id = ? AND user_id = ?", postId, userId);
        for (Integer idx : picked) {
            jdbcTemplate.update(
                    "INSERT INTO post_poll_votes(post_id, user_id, option_index) VALUES (?,?,?) ON CONFLICT DO NOTHING",
                    postId, userId, idx);
        }
        return buildView(post, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized Map<String, Object> closePoll(Long userId, Long postId) {
        requireLogin(userId);
        PostEntity post = requirePost(postId);
        if (!PostKindEnum.POLL.getCode().equals(kindOf(post))) {
            throw bad("该帖子不是投票");
        }
        if (!post.getUserId().equals(userId) && !isAdmin(userId)) {
            throw bad("只有楼主可以结束投票");
        }
        Map<String, Object> meta = parseMeta(post);
        meta.put("closed", true);
        meta.put("closedAt", OffsetDateTime.now().toString());
        saveMeta(post, meta);
        return buildView(post, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized Map<String, Object> acceptAnswer(Long userId, Long postId, Long replyId) {
        requireLogin(userId);
        PostEntity post = requirePost(postId);
        String kind = kindOf(post);
        if (!PostKindEnum.QUESTION.getCode().equals(kind) && !PostKindEnum.BOUNTY.getCode().equals(kind)) {
            throw bad("该帖子不是问答或悬赏");
        }
        if (!post.getUserId().equals(userId) && !isAdmin(userId)) {
            throw bad("只有楼主可以采纳回答");
        }
        PostEntity reply = requirePost(replyId);
        if (!PostTypeEnum.REPLY.getCode().equals(reply.getType())
                || reply.getReferencedPostId() == null
                || !reply.getReferencedPostId().equals(postId)) {
            throw bad("只能采纳本帖子下的回复");
        }
        if (reply.getUserId().equals(post.getUserId())) {
            throw bad("不能采纳自己的回答");
        }
        Map<String, Object> meta = parseMeta(post);
        if (meta.get("acceptedCommentId") != null) {
            throw bad("已经采纳过回答");
        }
        meta.put("acceptedCommentId", replyId);
        meta.put("solved", true);
        meta.put("solvedAt", OffsetDateTime.now().toString());
        if (PostKindEnum.BOUNTY.getCode().equals(kind)) {
            if (!bool(meta.get("escrowed"), false) || bool(meta.get("refunded"), false)) {
                throw bad("悬赏积分不在托管状态");
            }
            int points = intVal(meta.get("points"), 0);
            if (points <= 0) {
                throw bad("悬赏积分异常");
            }
            jdbcTemplate.update("UPDATE users SET points = points + ? WHERE id = ?", points, reply.getUserId());
            Long balance = jdbcTemplate.queryForObject("SELECT points FROM users WHERE id = ?", Long.class, reply.getUserId());
            jdbcTemplate.update(
                    "INSERT INTO point_records(user_id, type, points, balance, remark, created_at) VALUES (?,?,?,?,?,now())",
                    reply.getUserId(), PointTypeEnum.BOUNTY_AWARD.getCode(), points, balance, "悬赏采纳奖励 " + points + " 分");
            meta.put("escrowed", false);
            meta.put("awarded", true);
            meta.put("acceptedAt", OffsetDateTime.now().toString());
        }
        saveMeta(post, meta);
        return buildView(post, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized Map<String, Object> reopenQuestion(Long userId, Long postId) {
        requireLogin(userId);
        PostEntity post = requirePost(postId);
        if (!PostKindEnum.QUESTION.getCode().equals(kindOf(post))) {
            throw bad("该帖子不是问答");
        }
        if (!post.getUserId().equals(userId) && !isAdmin(userId)) {
            throw bad("只有楼主可以重新打开问题");
        }
        Map<String, Object> meta = parseMeta(post);
        meta.put("solved", false);
        meta.put("acceptedCommentId", null);
        meta.put("solvedAt", null);
        saveMeta(post, meta);
        return buildView(post, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized Map<String, Object> drawLottery(Long userId, Long postId) {
        requireLogin(userId);
        PostEntity post = requirePost(postId);
        if (!PostKindEnum.LOTTERY.getCode().equals(kindOf(post))) {
            throw bad("该帖子不是抽奖");
        }
        if (!post.getUserId().equals(userId)) {
            throw bad("只有楼主可以开奖");
        }
        Map<String, Object> meta = parseMeta(post);
        if (bool(meta.get("drawn"), false)) {
            throw bad("已经开过奖");
        }
        List<Long> entrants = jdbcTemplate.queryForList(
                "SELECT DISTINCT user_id FROM posts WHERE referenced_post_id = ? AND deleted_at IS NULL " +
                        "AND user_id <> ? AND type = 'reply'",
                Long.class, postId, post.getUserId());
        if (entrants.isEmpty()) {
            throw bad("还没有人参与抽奖");
        }
        Collections.shuffle(entrants, RANDOM);
        int slots = intVal(meta.get("slots"), 1);
        int winnerCount = Math.min(slots, entrants.size());
        List<Long> winners = new ArrayList<>(entrants.subList(0, winnerCount));
        meta.put("winnerIds", winners);
        meta.put("drawn", true);
        meta.put("closed", true);
        meta.put("drawnAt", OffsetDateTime.now().toString());
        saveMeta(post, meta);
        return buildView(post, userId);
    }

    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<Boolean> rows = jdbcTemplate.queryForList("SELECT admin FROM users WHERE id = ?", Boolean.class, userId);
        return !rows.isEmpty() && Boolean.TRUE.equals(rows.get(0));
    }

    private void fillPollView(PostEntity post, Map<String, Object> view, Map<String, Object> meta,
                              Long viewerId, boolean isAuthor, boolean admin) {
        List<?> options = meta.get("options") instanceof List<?> l ? l : List.of();
        List<Map<String, Object>> counts = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < options.size(); i++) {
            Long c = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM post_poll_votes WHERE post_id = ? AND option_index = ?",
                    Long.class, post.getId(), i);
            int n = c == null ? 0 : c.intValue();
            total += n;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", i);
            item.put("text", String.valueOf(options.get(i)));
            item.put("count", n);
            counts.add(item);
        }
        List<Integer> mine = viewerId == null ? List.of() : jdbcTemplate.queryForList(
                "SELECT option_index FROM post_poll_votes WHERE post_id = ? AND user_id = ? ORDER BY option_index",
                Integer.class, post.getId(), viewerId);
        boolean closed = bool(meta.get("closed"), false) || expired(meta.get("endsAt"));
        view.put("options", counts);
        view.put("totalVotes", total);
        view.put("myVotes", mine);
        view.put("voted", !mine.isEmpty());
        view.put("closed", closed);
        view.put("canVote", viewerId != null && !closed);
        view.put("canClose", (isAuthor || admin) && !closed);
    }

    private void fillLotteryView(PostEntity post, Map<String, Object> view, Map<String, Object> meta,
                                 Long viewerId, boolean isAuthor, boolean admin) {
        Long entrants = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM posts WHERE referenced_post_id = ? AND deleted_at IS NULL " +
                        "AND user_id <> ? AND type = 'reply'",
                Long.class, post.getId(), post.getUserId());
        view.put("entrants", entrants == null ? 0 : entrants.intValue());
        List<Long> winners = new ArrayList<>();
        if (meta.get("winnerIds") instanceof List<?> list) {
            for (Object o : list) {
                Long id = longVal(o);
                if (id != null) {
                    winners.add(id);
                }
            }
        }
        List<Map<String, Object>> winnerUsers = winners.stream()
                .map(this::userBrief)
                .filter(m -> m != null)
                .collect(Collectors.toList());
        view.put("winnerUsers", winnerUsers);

        // 奖品名额按槽位顺序绑定中奖用户：一等奖名额先用完，再轮到下一个奖品。
        List<Map<String, Object>> prizeItems = new ArrayList<>();
        int cursor = 0;
        if (meta.get("prizes") instanceof List<?> prizes) {
            for (Object pv : prizes) {
                if (!(pv instanceof Map<?, ?> pm)) {
                    continue;
                }
                String name = str(pm.get("name"));
                int count = intVal(pm.get("count"), 0);
                List<Map<String, Object>> prizeWinners = new ArrayList<>();
                for (int j = 0; j < count && cursor < winners.size(); j++) {
                    Map<String, Object> brief = userBrief(winners.get(cursor));
                    if (brief != null) {
                        prizeWinners.add(brief);
                    }
                    cursor++;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", name);
                item.put("count", count);
                item.put("winners", prizeWinners);
                prizeItems.add(item);
            }
        }
        view.put("prizes", prizeItems);
        boolean drawn = bool(meta.get("drawn"), false);
        view.put("canDraw", isAuthor && !drawn && entrants != null && entrants > 0);
        view.put("joined", viewerId != null && jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE referenced_post_id = ? AND user_id = ? AND deleted_at IS NULL",
                Long.class, post.getId(), viewerId) > 0);
    }

    private Map<String, Object> userBrief(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, nickname, avatar, avatar_thumb FROM users WHERE id = ?", userId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.get("id"));
        out.put("username", row.get("username"));
        out.put("nickname", row.get("nickname"));
        out.put("avatar", row.get("avatar"));
        out.put("avatarThumb", row.get("avatar_thumb"));
        return out;
    }

    private void saveMeta(PostEntity post, Map<String, Object> meta) {
        try {
            post.setInteractMeta(objectMapper.writeValueAsString(meta));
        } catch (Exception e) {
            throw bad("互动状态保存失败");
        }
        postMapper.update(post);
    }

    private PostEntity requirePost(Long postId) {
        PostEntity post = postMapper.selectOneById(postId);
        if (post == null || post.getDeletedAt() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "帖子不存在");
        }
        return post;
    }

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
    }

    public Map<String, Object> parseMeta(PostEntity post) {
        String json = post == null ? null : post.getInteractMeta();
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public Long acceptedReplyId(PostEntity post) {
        return longVal(parseMeta(post).get("acceptedCommentId"));
    }

    public String kindLabel(String kind) {
        PostKindEnum e = PostKindEnum.of(kind);
        return e == null ? PostKindEnum.DISCUSSION.getDesc() : e.getDesc();
    }

    private static boolean bool(Object v, boolean def) {
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return "true".equalsIgnoreCase(s) || "1".equals(s);
        if (v instanceof Number n) return n.intValue() != 0;
        return def;
    }

    private static int intVal(Object v, int def) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception ignored) {
                return def;
            }
        }
        return def;
    }

    private static Long longVal(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }

    private static boolean expired(Object endsAt) {
        String s = str(endsAt);
        if (s == null || s.isBlank()) {
            return false;
        }
        try {
            return OffsetDateTime.parse(s).isBefore(OffsetDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    private BusinessException bad(String msg) {
        return new BusinessException(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    public record Normalized(String kind, String metaJson) {
    }
}
