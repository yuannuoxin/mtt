package com.mtd.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * 第三方登录绑定
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_user_auth_third")
@FieldNameConstants
public class UserAuthThird extends BaseEntity {

    private Long userId;
    /**
     * 平台：wechat/qq/github/google
     */
    private String platform;
    /**
     * 平台唯一标识
     */
    private String openid;
    /**
     * 平台统一标识
     */
    private String unionid;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 状态
     */
    private Integer status;
}
