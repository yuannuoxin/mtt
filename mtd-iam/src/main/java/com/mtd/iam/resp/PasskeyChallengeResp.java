package com.mtd.iam.resp;

import lombok.Data;

/**
 * Passkey 注册挑战值响应
 */
@Data
public class PasskeyChallengeResp {
    
    /**
     * 挑战值 (Base64URL 编码)
     */
    private String challenge;
    
    /**
     * Relying Party ID
     */
    private String rpId;
    
    /**
     * Relying Party 名称
     */
    private String rpName;
    
    /**
     * 用户 ID (Base64URL 编码)
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 用户显示名称
     */
    private String userDisplayName;
}
