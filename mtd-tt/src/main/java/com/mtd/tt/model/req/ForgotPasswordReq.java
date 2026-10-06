package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 忘记密码（邮箱验证码重置密码）请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ForgotPasswordReq extends BaseRequest {

    @Parameter(description = "邮箱", required = true)
    private String email;

    @Parameter(description = "邮箱验证码", required = true)
    private String emailCode;

    @Parameter(description = "新密码（6-32 位）", required = true)
    private String newPassword;

    @Parameter(description = "Turnstile 人机校验 token（后端启用 Turnstile 时必填）")
    private String turnstileToken;
}
