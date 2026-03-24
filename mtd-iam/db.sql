CREATE
DATABASE IF NOT EXISTS mtd_iam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE
mtd_iam;


CREATE TABLE `iam_user`
(
    `id`          bigint  NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    varchar(64)      DEFAULT NULL COMMENT '登录用户名',
    `nickname`    varchar(64)      DEFAULT NULL COMMENT '昵称',
    `avatar`      varchar(255)     DEFAULT NULL COMMENT '头像',
    `status`      tinyint NOT NULL DEFAULT 1 COMMENT '1正常 2禁用 3注销',
    `create_time` datetime         DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint(20) DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11) DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';


CREATE TABLE `iam_user_credential`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `password`    varchar(128) NOT NULL COMMENT 'bcrypt/argon2加密密码',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint(20) DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11) DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户密码凭证';


CREATE TABLE `iam_user_contact`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT,
    `user_id`      bigint       NOT NULL,
    `contact_type` varchar(20)  NOT NULL COMMENT 'phone/email',
    `contact`      varchar(128) NOT NULL,
    -- 手机号专用：国家区号
    `country_code` varchar(10)           DEFAULT NULL COMMENT '国家区号 如 +86 / +1 / +44 / +852',
    `major`      tinyint      NOT NULL DEFAULT 0 COMMENT '1主账号 0辅助',
    `verified`     tinyint      NOT NULL DEFAULT 0 COMMENT '是否验证',
    `status`       tinyint      NOT NULL DEFAULT 1,
    `create_time`  datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    bigint(20) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`    bigint(20) DEFAULT NULL COMMENT '更新人 ID',
    `deleted`      int(11) DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY            `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户联系方式（多手机/多邮箱）';


CREATE TABLE `iam_user_mfa`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `secret`      varchar(128) NOT NULL COMMENT 'TOTP密钥',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '0关闭 1开启',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint(20) DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11) DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TOTP多因素认证（一人一个）';


CREATE TABLE `iam_user_passkey`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT,
    `user_id`       bigint       NOT NULL,
    `credential_id` varchar(255) NOT NULL COMMENT 'WebAuthn凭证ID',
    `public_key`    text         NOT NULL,
    `device_name`   varchar(100)          DEFAULT NULL COMMENT '设备名',
    `transports`    varchar(100)          DEFAULT NULL COMMENT 'usb/ble/nfc/internal',
    `status`        tinyint      NOT NULL DEFAULT 1,
    `create_time`   datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint(20) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`     bigint(20) DEFAULT NULL COMMENT '更新人 ID',
    `deleted`       int(11) DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_credential_id` (`credential_id`),
    KEY             `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Passkey/WebAuthn（一人多设备）';


CREATE TABLE `iam_user_auth_third`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `user_id`     bigint       NOT NULL,
    `platform`    varchar(32)  NOT NULL COMMENT 'wechat/qq/github/google',
    `openid`      varchar(128) NOT NULL,
    `unionid`     varchar(128)          DEFAULT NULL,
    `nickname`    varchar(64)           DEFAULT NULL,
    `avatar`      varchar(255)          DEFAULT NULL,
    `status`      tinyint      NOT NULL DEFAULT 1,
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint(20) DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11) DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_openid` (`platform`,`openid`),
    KEY           `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方登录绑定';

CREATE TABLE `iam_user_verify_code`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `receive`     varchar(128) NOT NULL COMMENT '手机号/邮箱',
    `country_code` varchar(10)           DEFAULT NULL COMMENT '国家区号 如 +86 / +1 / +44 / +852',
    `code`        varchar(16)  NOT NULL,
    `type`        varchar(20)  NOT NULL COMMENT 'sms/email',
    `scene`       varchar(32)  NOT NULL COMMENT 'login/register/reset_pwd/bind',
    `used`        tinyint      NOT NULL DEFAULT 0,
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint(20) DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11) DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY           `idx_receive` (`receive`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信/邮箱验证码';

CREATE TABLE `iam_user_access_log`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT,
    `user_id`     bigint      NOT NULL,
    `action`      varchar(32) NOT NULL COMMENT 'login/logout/change_pwd等',
    `ip`          varchar(64)          DEFAULT NULL,
    `ip_location` varchar(100)         DEFAULT NULL,
    `device`      varchar(255)         DEFAULT NULL,
    `status`      tinyint     NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    `create_time` datetime             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint(20) DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   bigint(20) DEFAULT NULL COMMENT '更新人 ID',
    `deleted`     int(11) DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_created_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录&访问日志';
