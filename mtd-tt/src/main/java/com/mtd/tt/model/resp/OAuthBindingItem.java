package com.mtd.tt.model.resp;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 已绑定的三方账号详情
 */
@Data
public class OAuthBindingItem implements Serializable {
    private String provider;
    private String providerName;
    private String email;
    private String displayName;
    private String avatar;
    private OffsetDateTime boundAt;
}
