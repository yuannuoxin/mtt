CREATE
DATABASE IF NOT EXISTS mtd_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE
mtd_demo;

CREATE TABLE sys_user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码',
    nickname    VARCHAR(50) COMMENT '昵称',
    email       VARCHAR(100) COMMENT '邮箱',
    phone       VARCHAR(20) COMMENT '手机号',
    status      INT      DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT COMMENT '创建人 ID',
    update_by   BIGINT COMMENT '更新人 ID',
    deleted     INT      DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
