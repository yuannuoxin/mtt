package com.mtd.tt.model.req;

import lombok.Data;

/** Google OIDC 登录（隐式流）请求：前端授权回调拿到 id_token 后交由后端本地验签 */
@Data
public class OAuthOidcReq {
    private String idToken;
}
