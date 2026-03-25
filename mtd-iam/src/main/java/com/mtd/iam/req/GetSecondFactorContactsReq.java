package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 获取二次验证联系方式请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetSecondFactorContactsReq extends BaseRequest {

    @NotBlank(message = "会话 ID 不能为空")
    @Parameter(description = "会话 ID（登录接口返回的 secondFactorId）", required = true)
    private String sessionId;

    @NotBlank(message = "验证类型不能为空")
    @Parameter(description = "验证类型（email/sms）", required = true, example = "email")
    private String secondFactorType;
}
