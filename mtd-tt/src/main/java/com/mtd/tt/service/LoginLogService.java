package com.mtd.tt.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.tt.enums.LoginProviderEnum;
import com.mtd.tt.mapper.LoginLogMapper;
import com.mtd.tt.model.entity.LoginLogEntity;
import com.mtd.tt.model.resp.LoginLogResp;
import com.mtd.tt.util.DeviceUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 登录日志：登录成功后记录来源方式、浏览器指纹、IP、UA，供用户端"登录记录"页查看当前/历史设备登录情况。
 * 同时把本次登录的设备信息写入当前 token 会话，供"登录设备"页展示与远程下线。
 */
@Slf4j
@Service
public class LoginLogService {

    /** token 会话属性：设备名 */
    public static final String SESSION_NAME = "dev:name";
    /** token 会话属性：IP */
    public static final String SESSION_IP = "dev:ip";
    /** token 会话属性：UA */
    public static final String SESSION_UA = "dev:ua";
    /** token 会话属性：浏览器指纹 */
    public static final String SESSION_FP = "dev:fp";
    /** token 会话属性：登录方式 */
    public static final String SESSION_PROVIDER = "dev:provider";

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

    /**
     * 记录一次登录（provider 如 password/email_code/google/github/telegram/discord），
     * 并把设备信息写入当前 token 会话（调用时需已完成 StpUtil.login）。
     */
    public void record(Long userId, String provider, String fingerprint) {
        if (userId == null) {
            return;
        }
        String ip = DeviceUtil.clientIp();
        String ua = DeviceUtil.userAgent();
        try {
            LoginLogEntity rec = new LoginLogEntity();
            rec.setUserId(userId);
            rec.setProvider(provider);
            rec.setFingerprint(fingerprint);
            rec.setIp(ip);
            rec.setUserAgent(ua);
            rec.setCreatedAt(OffsetDateTime.now());
            loginLogMapper.insertSelective(rec);
        } catch (Exception e) {
            log.warn("记录登录日志失败 user={} provider={}: {}", userId, provider, e.getMessage());
        }
        try {
            SaSession session = StpUtil.getTokenSession();
            session.set(SESSION_NAME, DeviceUtil.deviceName(ua));
            session.set(SESSION_IP, ip);
            session.set(SESSION_UA, ua);
            session.set(SESSION_FP, fingerprint);
            session.set(SESSION_PROVIDER, provider);
        } catch (Exception e) {
            log.warn("写入设备会话信息失败 user={}: {}", userId, e.getMessage());
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
}
