package com.mtd.iam.resp.tmp;

import lombok.Data;

@Data
public class WebAuthnLoginRequest {
    private String credentialId;
    private String clientDataJSON;
    private String authenticatorData;
    private String signature;
}
