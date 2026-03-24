package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 手机号注册请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PhoneRegisterRequest extends BaseRequest {

    private String countryCode;

    @NotBlank(message = "手机号不能为空")
    @Parameter(description = "手机号", required = true)
    private String phone;

    @NotNull(message = "验证码id不能为空")
    @Parameter(description = "验证码id", required = true)
    private Long verifyId;

    @NotBlank(message = "验证码不能为空")
    @Parameter(description = "验证码", required = true)
    private String code;

}
