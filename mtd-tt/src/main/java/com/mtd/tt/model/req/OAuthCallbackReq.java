package com.mtd.tt.model.req;

import lombok.Data;

@Data
public class OAuthCallbackReq {
    private String code;
    private String redirectUri;
}
