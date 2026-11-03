package com.mtd.tt.model.req;

import lombok.Data;

@Data
public class OAuthCallbackReq {
    private String code;
    private String redirectUri;
    /** 授权请求中携带的 nonce（防重放），后端从换回的 id_token 中校验一致 */
    private String nonce;
    /** OIDC PKCE：前端生成的 code_verifier，换 token 时回传（如 Telegram） */
    private String codeVerifier;

    /** 浏览器指纹（FingerprintJS v5 MIT，三方登录注册新用户时记录） */
    private String fingerprint;
}
