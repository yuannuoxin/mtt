package com.mtd.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * Passkey/WebAuthn（一人多设备）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_user_passkey")
@FieldNameConstants
public class UserPasskey extends BaseEntity {

    private Long userId;
    /**
     * WebAuthn 凭证 ID
     */
    private String credentialId;
    /**
     * 公钥
     */
    private String publicKey;
    /**
     * 设备名
     */
    private String deviceName;
    /**
     * 传输方式：usb/ble/nfc/internal
     */
    private String transports;
    /**
     * 状态
     */
    private Integer status;
}
