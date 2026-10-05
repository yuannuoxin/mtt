package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 注册请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterReq extends BaseRequest {

    @Parameter(description = "用户名", required = true)
    private String username;

    @Parameter(description = "昵称", required = true)
    private String nickname;

    @Parameter(description = "邮箱", required = true)
    private String email;

    @Parameter(description = "密码", required = true)
    private String password;

    @Parameter(description = "邮箱验证码（后端启用邮箱验证码注册时必填）")
    private String emailCode;

    @Parameter(description = "Turnstile 人机校验 token（后端启用 Turnstile 时必填）")
    private String turnstileToken;
}
