-- v0.05: 积分 + 管理员 + 经验等级系统
-- 覆盖最近的 schema 变更：积分、管理员权限、经验与等级，以及两张变动记录表。
-- 与 SysConfigService / PointService / ExpService 启动时自动建表逻辑保持一致。

-- 1. users 加列：积分 / 管理员 / 经验 / 等级
ALTER TABLE users ADD COLUMN IF NOT EXISTS points BIGINT NOT NULL DEFAULT 0;  -- 积分余额（发帖/转发/引用扣10、回复扣2、上传按1MB扣1、签到+1、注册送100）
ALTER TABLE users ADD COLUMN IF NOT EXISTS admin   BOOLEAN NOT NULL DEFAULT FALSE; -- 管理员（sysConfig/set 等需要）
ALTER TABLE users ADD COLUMN IF NOT EXISTS exp     BIGINT NOT NULL DEFAULT 0;   -- 累计经验（发帖+5、引用+3、转发+2、回复+1）
ALTER TABLE users ADD COLUMN IF NOT EXISTS level   INT    NOT NULL DEFAULT 1;   -- 积分等级（达到 exp.level.thresholds 挡位自动升级）

-- 2. point_records 积分变动记录（每笔扣/加积分落一条，含变动后余额）
CREATE TABLE IF NOT EXISTS point_records
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    type       VARCHAR(30) NOT NULL,   -- 类型枚举 PointTypeEnum：checkin/post/reply/upload/register/...
    points     BIGINT      NOT NULL,   -- 变动值（扣分为负、获得为正）
    balance    BIGINT      NOT NULL,   -- 变动后的积分余额
    remark     VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_point_records_user ON point_records (user_id, created_at DESC);

-- 3. exp_records 经验变动记录（发帖/引用/转发/回复各类型，含变动后累计经验）
CREATE TABLE IF NOT EXISTS exp_records
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    type       VARCHAR(30) NOT NULL,   -- 类型枚举 ExpTypeEnum：original/quote/repost/reply
    points     BIGINT      NOT NULL,   -- 获得经验（正数）
    balance    BIGINT      NOT NULL,   -- 变动后的累计经验
    remark     VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_exp_records_user ON exp_records (user_id, created_at DESC);
