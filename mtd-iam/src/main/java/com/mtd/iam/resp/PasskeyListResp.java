package com.mtd.iam.resp;

import lombok.Data;

import java.util.List;

/**
 * Passkey 列表响应
 */
@Data
public class PasskeyListResp {
    
    /**
     * 设备数量
     */
    private Integer count;
    
    /**
     * Passkey 设备列表
     */
    private List<PasskeyItemResp> passkeys;
}
