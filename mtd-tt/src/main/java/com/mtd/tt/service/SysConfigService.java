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
                   ('turnstile.site_key', '', 'Turnstile 站点 Site Key（前端渲染 widget 用）'),
                   ('turnstile.secret_key', '', 'Turnstile 站点 Secret Key（后端 siteverify 用）'),
                   ('email_code.enabled', 'false', '是否启用邮箱验证码注册（true/false，开发期默认关闭）'),
                   ('email.smtp.host', '', 'SMTP 主机'),
                   ('email.smtp.port', '465', 'SMTP 端口'),
                   ('email.smtp.username', '', 'SMTP 账号'),
                   ('email.smtp.password', '', 'SMTP 密码'),
                   ('email.smtp.from', '', '发件人邮箱')
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
