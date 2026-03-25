package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 二次验证请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SecondFactorVerifyRequest extends BaseRequest {

    @NotBlank(message = "验证 ID 不能为空")
    @Parameter(description = "验证 ID（登录接口返回的 secondFactorId）", required = true)
    private String sessionId;

    @NotBlank(message = "验证码不能为空")
    @Parameter(description = "验证码（邮箱/短信验证码或 MFA 动态码）", required = true)
    private String code;

    @Parameter(description = "验证类型（mfa/email/sms，第一次调用时必填）")
    private String secondFactorType;

    private Long verifyCodeId;
}
