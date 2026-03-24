package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Passkey 挑战值请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PasskeyChallengeRequest extends BaseRequest {

    @Parameter(description = "Relying Party ID (域名)")
    private String rpId;
}
