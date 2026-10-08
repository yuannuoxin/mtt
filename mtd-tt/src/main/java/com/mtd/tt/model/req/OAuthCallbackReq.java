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
}
