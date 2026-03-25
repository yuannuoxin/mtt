package com.mtd.iam.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二次验证选项
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecondFactorOption implements java.io.Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 联系方式 ID
     */
    private Long id;
    
    /**
     * 验证类型（mfa/email/sms）
     */
    private String type;
    
    /**
     * 验证描述
     */
    private String desc;
    
    /**
     * 联系方式（脱敏）
     */
    private String contact;
    
    /**
     * 国家区号（手机号需要）
     */
    private String countryCode;
}
