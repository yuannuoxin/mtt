package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Passkey 登录请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PasskeyLoginRequest extends BaseRequest {

    @NotBlank(message = "凭证 ID 不能为空")
    @Parameter(description = "WebAuthn 凭证 ID", required = true)
    private String credentialId;

    @NotBlank(message = "客户端数据不能为空")
    @Parameter(description = "Client Data JSON (Base64 编码)", required = true)
    private String clientDataJSON;

    @NotBlank(message = "认证器数据不能为空")
    @Parameter(description = "Authenticator Data (Base64 编码)", required = true)
    private String authenticatorData;

    @NotBlank(message = "签名不能为空")
    @Parameter(description = "签名数据 (Base64 编码)", required = true)
    private String signature;
    
    @Parameter(description = "Relying Party ID (域名)")
    private String rpId;
}
