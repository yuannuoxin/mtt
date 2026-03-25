package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密码登录请求参数（支持账号/邮箱/手机号）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordLoginRequest extends BaseRequest {

    @NotBlank(message = "账号不能为空")
    @Parameter(description = "账号（用户名/邮箱/手机号）", required = true)
    private String account;

    @NotBlank(message = "密码不能为空")
    @Parameter(description = "密码", required = true)
    private String password;

    @Parameter(description = "国家区号（手机号登录时必填，如 +86）")
    private String countryCode;
}
