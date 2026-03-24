package com.mtd.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * 用户信息扩展表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_user")
@FieldNameConstants
public class User extends BaseEntity {


    private String username;
    private String nickname;
    private String avatar;
    /**
     * 状态: 1正常 0禁用 3注销
     */
    private Integer status;
}
