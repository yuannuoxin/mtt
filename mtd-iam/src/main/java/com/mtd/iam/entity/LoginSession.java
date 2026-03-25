package com.mtd.iam.entity;

import lombok.Data;

/**
 * 登录会话信息
 */
@Data
public class LoginSession implements java.io.Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String sessionId;
    private Long userId;
}
