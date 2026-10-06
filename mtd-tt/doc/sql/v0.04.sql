-- v0.04：用户头像/横幅压缩图
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_thumb TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS banner_thumb TEXT;
