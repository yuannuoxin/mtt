package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发送验证码请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SendVerifyCodeRequest extends BaseRequest {

    @Parameter(description = "手机号或邮箱", required = true)
    private String receive;

    @Parameter(description = "国家区号（手机号专用，如 +86 / +1 / +44）")
    private String countryCode;

    @NotBlank(message = "验证码类型不能为空")
    @Parameter(description = "验证码类型：sms-短信/email-邮箱", required = true)
    private String type;

    @NotBlank(message = "使用场景不能为空")
    @Parameter(description = "使用场景：login-登录/register-注册/reset_pwd-重置密码/bind-绑定")
    private String scene;
}
