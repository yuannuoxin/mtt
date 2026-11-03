package com.mtd.tt.service;

import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.tt.enums.LoginProviderEnum;
import com.mtd.tt.mapper.LoginLogMapper;
import com.mtd.tt.model.entity.LoginLogEntity;
import com.mtd.tt.model.resp.LoginLogResp;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 登录日志：登录成功后记录来源方式、浏览器指纹、IP、UA，供用户端"登录记录"页查看当前/历史设备登录情况。
 */
@Slf4j
@Service
public class LoginLogService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private LoginLogMapper loginLogMapper;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS login_logs
                    (
                        id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        user_id    BIGINT       NOT NULL,
                        provider   VARCHAR(30)  NOT NULL,
                        fingerprint VARCHAR(128),
                        ip         VARCHAR(64),
                        user_agent VARCHAR(255),
                        created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_login_logs_user ON login_logs (user_id, created_at DESC)");
            log.info("登录日志表初始化完成");
        } catch (Exception e) {
            log.error("登录日志表初始化失败: {}", e.getMessage(), e);
        }
    }

    /** 记录一次登录（provider 如 password/email_code/google/github/telegram/discord） */
    public void record(Long userId, String provider, String fingerprint) {
        if (userId == null) {
            return;
        }
        try {
            LoginLogEntity rec = new LoginLogEntity();
            rec.setUserId(userId);
            rec.setProvider(provider);
            rec.setFingerprint(fingerprint);
            rec.setIp(clientIp());
            rec.setUserAgent(userAgent());
            rec.setCreatedAt(OffsetDateTime.now());
            loginLogMapper.insertSelective(rec);
        } catch (Exception e) {
            log.warn("记录登录日志失败 user={} provider={}: {}", userId, provider, e.getMessage());
        }
    }

    /** 当前用户的登录记录（分页，按时间倒序） */
    public PageResponse<LoginLogResp> records(Long userId, PageRequest page) {
        com.mybatisflex.core.paginate.Page<LoginLogEntity> p =
                loginLogMapper.paginate(page.getPageNum(), page.getPageSize(),
                        com.mybatisflex.core.query.QueryWrapper.create()
                                .eq(LoginLogEntity::getUserId, userId)
                                .orderBy(LoginLogEntity::getCreatedAt, false)
                                .orderBy(LoginLogEntity::getId, false));
        List<LoginLogResp> list = p.getRecords().stream().map(this::toResp).toList();
        return PageResponse.of((int) p.getPageNumber(), (int) p.getPageSize(), p.getTotalRow(), list);
    }

    private LoginLogResp toResp(LoginLogEntity e) {
        LoginLogResp r = new LoginLogResp();
        r.setId(e.getId());
        r.setProvider(e.getProvider());
        LoginProviderEnum t = LoginProviderEnum.of(e.getProvider());
        r.setProviderDesc(t == null ? e.getProvider() : t.getDesc());
        r.setFingerprint(e.getFingerprint());
        r.setIp(e.getIp());
        r.setUserAgent(e.getUserAgent());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            return first.length() > 64 ? first.substring(0, 64) : first;
        }
        return req.getRemoteAddr();
    }

    private String userAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String ua = attrs.getRequest().getHeader("User-Agent");
        if (ua == null || ua.isBlank()) {
            return null;
        }
        return ua.length() > 255 ? ua.substring(0, 255) : ua;
    }
}
