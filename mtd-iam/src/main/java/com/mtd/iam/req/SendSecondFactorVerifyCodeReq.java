package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发送二次验证验证码请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SendSecondFactorVerifyCodeReq extends BaseRequest {

    @NotNull(message = "联系方式 ID 不能为空")
    @Parameter(description = "联系方式 ID（登录接口返回的 secondFactorId）", required = true)
    private Long contactId;

    @NotBlank(message = "会话 ID 不能为空")
    @Parameter(description = "会话 ID（登录接口返回的 secondFactorId）", required = true)
    private String sessionId;
}
