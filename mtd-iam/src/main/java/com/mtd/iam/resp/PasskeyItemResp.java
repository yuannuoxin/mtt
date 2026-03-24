package com.mtd.iam.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Passkey 设备信息响应
 */
@Data
public class PasskeyItemResp {
    
    /**
     * 设备 ID
     */
    private Long id;
    
    /**
     * 凭证 ID
     */
    private String credentialId;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 传输方式
     */
    private String transports;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
