package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Passkey 注册请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PasskeyRegisterRequest extends BaseRequest {

    @NotBlank(message = "凭证 ID 不能为空")
    @Parameter(description = "WebAuthn 凭证 ID", required = true)
    private String credentialId;

    @NotBlank(message = "公钥不能为空")
    @Parameter(description = "公钥 (Base64 编码)", required = true)
    private String publicKey;

    @NotBlank(message = "客户端数据不能为空")
    @Parameter(description = "Client Data JSON (Base64 编码)", required = true)
    private String clientDataJSON;

    @NotBlank(message = "认证器数据不能为空")
    @Parameter(description = "Authenticator Data (Base64 编码)", required = true)
    private String authenticatorData;

    @Parameter(description = "设备名称")
    private String deviceName;

    @Parameter(description = "传输方式：usb/ble/nfc/internal")
    private String transports;
    
    @Parameter(description = "Relying Party ID (域名)")
    private String rpId;
}
