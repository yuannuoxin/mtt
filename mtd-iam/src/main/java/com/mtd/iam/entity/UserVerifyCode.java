package com.mtd.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * 短信/邮箱验证码
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_user_verify_code")
@FieldNameConstants
public class UserVerifyCode extends BaseEntity {

    /**
     * 手机号/邮箱
     */
    private String receive;

    private String countryCode;
    /**
     * 验证码
     */
    private String code;
    /**
     * 类型：sms/email
     */
    private String type;
    /**
     * 场景：login/register/reset_pwd/bind
     */
    private String scene;
    /**
     * 是否已使用：0-未使用 1-已使用
     */
    private Integer used;
}
