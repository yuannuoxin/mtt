-- v0.03: 文件上传治理 + 推文媒体关联文件
-- 1. 用户文件表：记录每次上传的文件，发推后可回填关联到具体推文
CREATE TABLE user_files (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    scene        VARCHAR(32) NOT NULL,              -- 场景枚举 FileSceneEnum: avatar/banner/post/...
    media_type   VARCHAR(20),                       -- image/video/audio（上传时可知）
    object_key   VARCHAR(255) NOT NULL,             -- MinIO 对象 key（相对路径）
    url          TEXT        NOT NULL,              -- 可访问 URL
    size         BIGINT,
    content_type VARCHAR(100),
    post_id      BIGINT,                            -- 归属推文，发推后回填
    thumbnail BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_files_user ON user_files (user_id);
CREATE INDEX idx_user_files_url ON user_files (url);
CREATE INDEX idx_user_files_post ON user_files (post_id);
CREATE INDEX idx_user_files_scene ON user_files (scene);

-- 2. 推文媒体表加文件 ID 字段（指向 user_files.id）
ALTER TABLE post_medias ADD COLUMN original_file_id BIGINT;
ALTER TABLE post_medias ADD COLUMN thumbnail_file_id BIGINT;
ALTER TABLE post_medias ADD COLUMN play_file_id BIGINT;
