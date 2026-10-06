package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改邮箱请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeEmailReq extends BaseRequest {

    @Parameter(description = "新邮箱", required = true)
    private String email;

    @Parameter(description = "新邮箱验证码", required = true)
    private String emailCode;

    @Parameter(description = "Turnstile 人机校验 token（后端启用 Turnstile 时必填）")
    private String turnstileToken;
}
