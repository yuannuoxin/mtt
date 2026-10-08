package com.mtd.tt.service;

import com.mtd.tt.mapper.SysConfigMapper;
import com.mtd.tt.model.entity.SysConfigEntity;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统控制配置服务：管理 sys_config 表，为 Turnstile 人机校验、邮箱验证码等提供运行时开关。
 * 启动时自动建表并写入默认值，后续通过缓存读写，改配置即时生效。
 */
@Slf4j
@Service
public class SysConfigService {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS sys_config
            (
                id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                cfg_key    VARCHAR(100) NOT NULL UNIQUE,
                cfg_value  TEXT,
                remark     VARCHAR(255),
                updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
            )
            """;

    private static final String INIT_SQL = """
            INSERT INTO sys_config (cfg_key, cfg_value, remark)
            VALUES ('turnstile.enabled', 'false', '是否启用 Cloudflare Turnstile 人机校验（true/false，开发期默认关闭）'),
                   ('turnstile.mode', 'non_interactive', 'Turnstile 模式：managed(托管/交互) / invisible(隐形) / non_interactive(非交互)'),
                   ('turnstile.site_key', '', 'Turnstile 站点 Site Key（前端渲染 widget 用，non_interactive 模式使用）'),
                   ('turnstile.secret_key', '', 'Turnstile 站点 Secret Key（后端 siteverify 用，non_interactive 模式使用）'),
                   ('turnstile.managed.site_key', '', 'Turnstile 托管(Managed)模式 Site Key'),
                   ('turnstile.managed.secret_key', '', 'Turnstile 托管(Managed)模式 Secret Key'),
                   ('turnstile.invisible.site_key', '', 'Turnstile 隐形(Invisible)模式 Site Key'),
                   ('turnstile.invisible.secret_key', '', 'Turnstile 隐形(Invisible)模式 Secret Key'),
                   ('captcha.provider', 'turnstile', '人机校验提供商：turnstile / recaptcha'),
                   ('recaptcha.mode', 'v3', 'reCAPTCHA 模式：v3 / v2 / v2_invisible'),
                   ('recaptcha.site_key', '', 'reCAPTCHA v3 Site Key'),
                   ('recaptcha.secret_key', '', 'reCAPTCHA v3 Secret Key'),
                   ('recaptcha.threshold', '0.5', 'reCAPTCHA v3 score 放行阈值（0~1）'),
                   ('recaptcha.v2.site_key', '', 'reCAPTCHA v2(复选框) Site Key'),
                   ('recaptcha.v2.secret_key', '', 'reCAPTCHA v2(复选框) Secret Key'),
                   ('recaptcha.v2_invisible.site_key', '', 'reCAPTCHA v2 隐形 Site Key'),
                   ('recaptcha.v2_invisible.secret_key', '', 'reCAPTCHA v2 隐形 Secret Key'),
                   ('email_code.enabled', 'false', '是否启用邮箱验证码注册（true/false，开发期默认关闭）'),
                   ('email.smtp.host', '', 'SMTP 主机'),
                   ('email.smtp.port', '465', 'SMTP 端口'),
                   ('email.smtp.username', '', 'SMTP 账号'),
                   ('email.smtp.password', '', 'SMTP 密码'),
                   ('email.smtp.from', '', '发件人邮箱'),
                   ('email.domain.blacklist', '', '注册邮箱域名黑名单（逗号分隔，空=不限制）'),
                   ('points.checkin', '1', '每日签到送积分'),
                   ('points.post', '10', '发帖/转发/引用 扣积分'),
                   ('points.reply', '2', '回复扣积分'),
                   ('points.upload_per_mb', '1', '上传每1MB扣积分（不足1MB按1MB）'),
                   ('points.initial', '100', '新用户注册初始积分'),
                   ('exp.per.post', '5', '发帖获得经验'),
                   ('exp.per.quote', '3', '引用获得经验'),
                   ('exp.per.repost', '2', '转发获得经验'),
                   ('exp.per.reply', '1', '回复获得经验'),
                   ('exp.level.thresholds', '100,300,600,1000', '经验等级挡位（逗号分隔：升到2/3/4/5级所需累计经验）'),
                   ('oauth.google.enabled', 'false', '是否启用 Google 三方登录（true/false）'),
                   ('oauth.google.client_id', '', 'Google OAuth Client ID'),
                   ('oauth.google.client_secret', '', 'Google OAuth Client Secret'),
                   ('oauth.google.redirect_uri', '', 'Google OAuth 回调地址（前端 /oauth/google/callback 完整 URL）'),
                   ('oauth.google.mode', 'code', 'Google OAuth 模式：code=后端用code换token(需与Google交互/走代理)；id_token=前端隐式流拿id_token后端本地验签(不交互)'),
                   ('oauth.github.enabled', 'false', 'GitHub 登录是否启用'),
                   ('oauth.github.client_id', '', 'GitHub OAuth App Client ID'),
                   ('oauth.github.client_secret', '', 'GitHub OAuth App Client Secret'),
                   ('oauth.github.redirect_uri', '', 'GitHub OAuth 回调地址（前端 /oauth/github/callback 完整 URL）'),
                   ('oauth.telegram.enabled', 'false', 'Telegram 登录是否启用'),
                   ('oauth.telegram.client_id', '', 'Telegram OIDC Client ID（BotFather Login Widget 提供）'),
                   ('oauth.telegram.client_secret', '', 'Telegram OIDC Client Secret'),
                   ('oauth.telegram.redirect_uri', '', 'Telegram OIDC 回调地址（前端 /oauth/telegram/callback 完整 URL）'),
                   ('oauth.discord.enabled', 'false', 'Discord 登录是否启用'),
                   ('oauth.discord.client_id', '', 'Discord OAuth2 Client ID'),
                   ('oauth.discord.client_secret', '', 'Discord OAuth2 Client Secret'),
                   ('oauth.discord.redirect_uri', '', 'Discord OAuth2 回调地址（前端 /oauth/discord/callback 完整 URL）'),
                   ('oauth.proxy.enabled', 'false', 'Google API 请求是否走代理（本机无法直连时开启 true）'),
                   ('oauth.proxy.host', '', '代理主机，如 127.0.0.1'),
                   ('oauth.proxy.port', '', '代理端口，如 10808')
            ON CONFLICT (cfg_key) DO NOTHING
            """;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    /** 缓存最后加载时间戳，超过间隔则自动从库刷新，支持直接改库即时生效 */
    private volatile long lastReload = 0;
    private static final long RELOAD_INTERVAL_MS = 30_000;
    private final Object reloadLock = new Object();

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute(CREATE_TABLE_SQL);
            jdbcTemplate.execute(INIT_SQL);
            // 用户表管理员标记（新用户默认非管理员；授权由后台 UPDATE users SET admin=true 完成）
            // 不使用 is_ 开头字段：admin 列；若存在旧 is_admin 列则迁移后删除
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS admin BOOLEAN NOT NULL DEFAULT FALSE");
            // 经验与积分等级（users.exp / users.level，无 is 前缀字段）
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS exp BIGINT NOT NULL DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS level INT NOT NULL DEFAULT 1");
            try {
                jdbcTemplate.update("UPDATE users SET admin = COALESCE(is_admin, FALSE)");
                jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS is_admin");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE user_files RENAME COLUMN is_thumbnail TO thumbnail");
            } catch (Exception ignored) {
            }
            reloadCache();
            log.info("sys_config 初始化完成，配置项 {} 条", cache.size());
        } catch (Exception e) {
            log.error("sys_config 初始化失败: {}", e.getMessage(), e);
        }
    }

    /** 缓存过期则重新从库加载（防抖），保证直接改库也能生效 */
    private void refreshIfStale() {
        if (System.currentTimeMillis() - lastReload > RELOAD_INTERVAL_MS) {
            synchronized (reloadLock) {
                if (System.currentTimeMillis() - lastReload > RELOAD_INTERVAL_MS) {
                    reloadCache();
                }
            }
        }
    }

    private void reloadCache() {
        cache.clear();
        List<SysConfigEntity> all = sysConfigMapper.selectListByQuery(
                QueryWrapper.create().orderBy(SysConfigEntity::getCfgKey, true));
        for (SysConfigEntity c : all) {
            if (c.getCfgValue() != null) {
                cache.put(c.getCfgKey(), c.getCfgValue());
            }
        }
        lastReload = System.currentTimeMillis();
    }

    /**
     * 读取配置，不存在返回 null
     */
    public String get(String key) {
        refreshIfStale();
        return cache.get(key);
    }

    /**
     * 读取布尔配置（true/false 或 1/0），默认值兜底
     */
    public boolean getBool(String key, boolean def) {
        refreshIfStale();
        String v = cache.get(key);
        if (v == null) {
            return def;
        }
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    /**
     * 读取整数配置，默认值兜底
     */
    public int getInt(String key, int def) {
        refreshIfStale();
        String v = cache.get(key);
        if (v == null) {
            return def;
        }
        try {
            return (int) Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * 更新配置并刷新缓存
     */
    public void set(String key, String value) {
        SysConfigEntity exist = sysConfigMapper.selectOneByQuery(
                QueryWrapper.create().eq(SysConfigEntity::getCfgKey, key));
        if (exist == null) {
            SysConfigEntity e = new SysConfigEntity();
            e.setCfgKey(key);
            e.setCfgValue(value);
            sysConfigMapper.insertSelective(e);
        } else {
            SysConfigEntity update = new SysConfigEntity();
            update.setId(exist.getId());
            update.setCfgValue(value);
            sysConfigMapper.update(update);
        }
        if (value != null) {
            cache.put(key, value);
        } else {
            cache.remove(key);
        }
    }
}
