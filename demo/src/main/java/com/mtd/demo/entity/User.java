package com.mtd.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@FieldNameConstants
public class User extends BaseEntity {

    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;
}
