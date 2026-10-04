-- 1. users 用户表
CREATE TABLE users
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)         NOT NULL UNIQUE,
    status        VARCHAR(10)         NOT NULL DEFAULT 'active',
    nickname      VARCHAR(100)        NOT NULL,
    email         VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(255)        NOT NULL,
    avatar        TEXT,
    banner        TEXT,
    bio           TEXT,
    user_private  BOOLEAN                      DEFAULT false,
    created_at    TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ NULL
);
CREATE INDEX idx_users_username ON users (username);

-- 2. follows 关注关系
CREATE TABLE follows
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    follower_id BIGINT      NOT NULL,
    followee_id BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (follower_id, followee_id),
    CHECK (follower_id != followee_id
)
    );
CREATE INDEX idx_follows_follower ON follows (follower_id);
CREATE INDEX idx_follows_followee ON follows (followee_id);

-- 3. blocks 拉黑
CREATE TABLE blocks
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    blocked_user_id BIGINT      NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, blocked_user_id),
    CHECK (user_id != blocked_user_id
)
    );
CREATE INDEX idx_blocks_user ON blocks (user_id);

-- 4. posts 主帖子表（核心）
CREATE TABLE posts
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT      NOT NULL,
    type               VARCHAR(20) NOT NULL,-- original/quote/repost/reply
    content            TEXT,
    referenced_post_id BIGINT NULL,
    root_post_id       BIGINT NULL,
    visibility         VARCHAR(20) NOT NULL DEFAULT 'public', -- public/followers_only
    article_flag       INT                  DEFAULT 0,
    has_media          BOOLEAN              DEFAULT false,
    has_image          BOOLEAN              DEFAULT false,
    has_video          BOOLEAN              DEFAULT false,
    reply_permission   VARCHAR(30)          DEFAULT 'anyone', -- anyone/followed/mentioned
    like_count         BIGINT               DEFAULT 0,
    reply_count        BIGINT               DEFAULT 0,
    repost_count       BIGINT               DEFAULT 0,
    view_count         BIGINT               DEFAULT 0,
    bookmark_count     BIGINT               DEFAULT 0,
    hot_score          BIGINT               DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMPTZ NULL
);
CREATE INDEX idx_posts_user_created ON posts (user_id, created_at);
CREATE INDEX idx_posts_root_post ON posts (root_post_id);
CREATE INDEX idx_posts_referenced_post ON posts (referenced_post_id);

-- 5. post_medias 媒体附件
CREATE TABLE post_medias
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id       BIGINT      NOT NULL,
    media_type    VARCHAR(20) NOT NULL,
    original_url  TEXT        NOT NULL,
    thumbnail_url TEXT,
    play_url      TEXT,
    width         INT,
    height        INT,
    duration      NUMERIC(8, 2) NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_post_medias_post ON post_medias (post_id);

-- 6. post_mentions @用户关联
CREATE TABLE post_mentions
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id      BIGINT      NOT NULL,
    mention_type VARCHAR(20) NOT NULL, -- 枚举：user @用户 | topic 话题
    target_id    BIGINT      NOT NULL, -- user则是user_id；topic则是topic_id
    start_idx    INT         NOT NULL, -- 在content文本里，起始字符下标（包含）
    end_idx      INT         NOT NULL, -- 在content文本里，结束字符下标（不包含）
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, mention_type, target_id, start_idx)
);
CREATE INDEX idx_post_mentions_post ON post_mentions (post_id);

-- 8. post_likes 点赞明细（唯一真值）
CREATE TABLE post_likes
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    post_id    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, post_id)
);
CREATE INDEX idx_post_likes_post ON post_likes (post_id);

-- 9. bookmarks 收藏
CREATE TABLE bookmarks
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    post_id    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, post_id)
);
CREATE INDEX idx_bookmarks_user ON bookmarks (user_id);

-- 10. notifications 通知
CREATE TABLE notifications
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    receiver_user_id BIGINT      NOT NULL,
    trigger_user_id  BIGINT      NOT NULL,
    type             VARCHAR(20) NOT NULL,
    post_id          BIGINT NULL,
    read             BOOLEAN              DEFAULT false,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_receiver ON notifications (receiver_user_id, read);

-- 11. user_timeline 关注时间线（首页正在关注流）
CREATE TABLE user_timeline
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    item_type    VARCHAR(20) NOT NULL,
    item_id      BIGINT      NOT NULL,
    item_user_id BIGINT      NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    valid        BOOLEAN DEFAULT true,
    UNIQUE (user_id, item_id)
);
CREATE INDEX idx_user_timeline_user ON user_timeline (user_id, created_at DESC);
CREATE INDEX idx_user_timeline_itemuser ON user_timeline (user_id, item_user_id);

-- 13. user_post_read 用户已读记录（推荐流去重）
CREATE TABLE user_post_read
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    post_id    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, post_id)
);
CREATE INDEX idx_user_post_read_user ON user_post_read (user_id);

-- 15. post_link_cards：新版卡片表（含card_type、视频相关字段）
CREATE TABLE post_link_cards
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id         BIGINT      NOT NULL,
    card_type       VARCHAR(20) NOT NULL DEFAULT 'link', -- 枚举 link / embed_video
    target_url      TEXT NULL,
    title           VARCHAR(255) NULL,                   -- 页面标题（og:title）
    description     VARCHAR(1000) NULL,                  -- 页面描述（og:description）
    video_provider  VARCHAR(20) NULL,                    -- 枚举 bilibili / youtube，仅card_type=embed_video生效
    video_id        VARCHAR(255) NULL,                   -- 第三方视频ID，仅card_type=embed_video生效
    cover_media_url TEXT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (post_id)
);
CREATE INDEX idx_post_link_cards_post ON post_link_cards (post_id);
