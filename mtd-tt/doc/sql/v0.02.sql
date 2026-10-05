-- v0.02：系统控制表（Turnstile 人机校验 / 邮箱验证码 开关与配置）
-- 通过控制表可随时开关能力，无需改代码重启，开发阶段默认关闭。
-- 表结构由 SysConfigService 在启动时自动创建并初始化，本文件为迁移文档。

CREATE TABLE IF NOT EXISTS sys_config
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cfg_key    VARCHAR(100) NOT NULL UNIQUE,
    cfg_value  TEXT,
    remark     VARCHAR(255),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

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
ON CONFLICT (cfg_key) DO NOTHING;
