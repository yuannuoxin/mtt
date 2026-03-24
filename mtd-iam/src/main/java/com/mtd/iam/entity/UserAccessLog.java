package com.mtd.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * 用户登录&访问日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_user_access_log")
@FieldNameConstants
public class UserAccessLog extends BaseEntity {

    private Long userId;
    /**
     * 动作：login/logout/change_pwd等
     */
    private String action;
    /**
     * IP 地址
     */
    private String ip;
    /**
     * IP 所在地
     */
    private String ipLocation;
    /**
     * 设备信息
     */
    private String device;
    /**
     * 状态：1-成功 0-失败
     */
    private Integer status;
}
