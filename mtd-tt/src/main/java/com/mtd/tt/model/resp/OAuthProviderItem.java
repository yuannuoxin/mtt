package com.mtd.tt.model.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统支持的一个三方提供商视图
 */
@Data
public class OAuthProviderItem implements Serializable {
    private String provider;
    private String name;
    /** 是否启用（后台配置 oauth.google.enabled） */
    private boolean enabled;
    /** 当前用户是否已绑定该三方 */
    private boolean bound;
    /** 前端跳转 Google 授权页所需 */
    private String clientId;
    private String redirectUri;
    /** OAuth 模式：code=后端换token(需交互)；id_token=前端隐式流拿id_token后端本地验签(不交互) */
    private String mode;
}
