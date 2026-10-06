package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改密码请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChangePasswordReq extends BaseRequest {

    @Parameter(description = "当前密码", required = true)
    private String oldPassword;

    @Parameter(description = "新密码（6-32 位）", required = true)
    private String newPassword;

    @Parameter(description = "Turnstile 人机校验 token（后端启用 Turnstile 时必填）")
    private String turnstileToken;
}
