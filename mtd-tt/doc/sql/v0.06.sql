-- v0.06: 三方登录（Google OAuth）
-- 新增 oauth_bindings 三方绑定表，并放开 users.password_hash 的 NOT NULL 约束
-- （Google/三方自动注册的用户没有密码，password_hash 允许为 NULL）。

-- 1. 三方绑定表：一个用户可绑多个三方；provider+provider_user_id 全局唯一，保证三方账号只能归属一个本地账号
CREATE TABLE IF NOT EXISTS oauth_bindings
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,      -- 三方枚举 OauthProviderEnum：google / ...
    provider_user_id VARCHAR(128) NOT NULL,      -- 三方平台的用户唯一 id
    email            VARCHAR(120),
    display_name     VARCHAR(100),
    avatar           VARCHAR(255),
    access_token     VARCHAR(512),
    refresh_token    VARCHAR(512),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_user_id)
);
CREATE INDEX IF NOT EXISTS idx_oauth_bindings_user ON oauth_bindings (user_id);

-- 2. 放开 users.password_hash 非空约束：三方登录自动注册的用户无密码
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
