-- 用户表
CREATE TABLE `sys_user` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `username`    varchar(50)  NOT NULL COMMENT '用户名',
    `password`    varchar(128) NOT NULL COMMENT '密码',
    `nickname`    varchar(50)           DEFAULT NULL COMMENT '昵称',
    `email`       varchar(100)          DEFAULT NULL COMMENT '邮箱',
    `phone`       varchar(20)           DEFAULT NULL COMMENT '手机号',
    `avatar`      varchar(255)          DEFAULT NULL COMMENT '头像',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11)               DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 角色表
CREATE TABLE `sys_role` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_code`   varchar(50)  NOT NULL COMMENT '角色编码',
    `role_name`   varchar(50)  NOT NULL COMMENT '角色名称',
    `description` varchar(200)          DEFAULT NULL COMMENT '描述',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11)               DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 权限/菜单表
CREATE TABLE `sys_permission` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `parent_id`   bigint                DEFAULT 0 COMMENT '父级ID',
    `perm_code`   varchar(100) NOT NULL COMMENT '权限编码',
    `perm_name`   varchar(50)  NOT NULL COMMENT '权限名称',
    `perm_type`   tinyint      NOT NULL DEFAULT 1 COMMENT '类型: 1菜单 2按钮 3接口',
    `path`        varchar(200)          DEFAULT NULL COMMENT '路由路径',
    `component`   varchar(200)          DEFAULT NULL COMMENT '组件路径',
    `icon`        varchar(50)           DEFAULT NULL COMMENT '图标',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11)               DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限/菜单表';

-- 用户角色关联表
CREATE TABLE `sys_user_role` (
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `role_id`     bigint   NOT NULL COMMENT '角色ID',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11)               DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE `sys_role_permission` (
    `id`            bigint   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id`       bigint   NOT NULL COMMENT '角色ID',
    `permission_id` bigint   NOT NULL COMMENT '权限ID',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11)               DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 插入默认数据
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `status`) 
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@example.com', 1);

INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `sort`) 
VALUES (1, 'admin', '超级管理员', '拥有所有权限', 1);

INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort`) 
VALUES 
(1, 0, 'system', '系统管理', 1, '/system', 'setting', 1),
(2, 1, 'system:user', '用户管理', 1, '/system/user', 'user', 1),
(3, 1, 'system:role', '角色管理', 1, '/system/role', 'team', 2),
(4, 1, 'system:permission', '权限管理', 1, '/system/permission', 'lock', 3);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) 
VALUES (1, 1), (1, 2), (1, 3), (1, 4);
