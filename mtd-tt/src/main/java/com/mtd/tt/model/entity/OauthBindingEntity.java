package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

/**
 * 第三方账号绑定：一个用户可绑定多个三方（google 等），支持登录与绑定/解绑。
 * provider + provider_user_id 全局唯一，保证三方账号只能归属一个本地用户。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("oauth_bindings")
@FieldNameConstants
public class OauthBindingEntity extends BaseEntity {
    private Long userId;
    /** 三方提供商：OauthProviderEnum（google / github ...） */
    private String provider;
    /** 三方平台的用户唯一 ID（Google sub） */
    private String providerUserId;
    private String email;
    private String displayName;
    private String avatar;
    private String accessToken;
    private String refreshToken;
    private OffsetDateTime createdAt;
}
