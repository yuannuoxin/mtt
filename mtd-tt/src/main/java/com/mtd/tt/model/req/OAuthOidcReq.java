package com.mtd.tt.model.req;

import lombok.Data;

/** Google OIDC 登录（隐式流）请求：前端授权回调拿到 id_token 后交由后端本地验签 */
@Data
public class OAuthOidcReq {
    private String idToken;
    /** 授权请求中携带的 nonce（防重放），后端需与 id_token 内 nonce 比对 */
    private String nonce;

    /** 浏览器指纹（FingerprintJS v5 MIT，三方登录注册新用户时记录） */
    private String fingerprint;
}
