package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

/**
 * 登录日志：记录每次登录的来源方式、浏览器指纹、IP、UA，用于用户在登录记录页核对自己设备的登录情况。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("login_logs")
@FieldNameConstants
public class LoginLogEntity extends BaseEntity {
    private Long userId;
    /** 登录方式：password / email_code / google / github / telegram / discord / register */
    private String provider;
    /** 登录时的浏览器指纹（FingerprintJS v5 MIT 前端生成） */
    private String fingerprint;
    private String ip;
    private String userAgent;
    private OffsetDateTime createdAt;
}
