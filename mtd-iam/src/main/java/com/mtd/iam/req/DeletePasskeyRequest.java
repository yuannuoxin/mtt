package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 删除 Passkey 请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeletePasskeyRequest extends BaseRequest {

    @NotBlank(message = "凭证 ID 不能为空")
    @Parameter(description = "WebAuthn 凭证 ID", required = true)
    private String credentialId;
}
