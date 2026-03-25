package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MFA 管理请求参数（添加/更新/删除通用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MfaManageRequest extends BaseRequest {

    @Parameter(description = "MFA 记录 ID（更新或删除时需要）")
    private Long mfaId;

    @Parameter(description = "验证码（用于验证身份）")
    private String code;

    @Parameter(description = "是否启用 MFA（1-启用，0-禁用，仅更新时需要）")
    private Integer status;
}
