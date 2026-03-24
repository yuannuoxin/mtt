package com.mtd.iam.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_user_credential")
@FieldNameConstants
public class UserCredential extends BaseEntity {
    private String password;
}
