package com.mtd.iam.resp.tmp;

import lombok.Data;

@Data
public class WebAuthnRegisterRequest {
    private String credentialId;
    private String publicKey;
    private String clientDataJSON;
    private String authenticatorData;
    private String deviceName;
    private String transports;
}
