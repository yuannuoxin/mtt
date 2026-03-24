package com.mtd.iam.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邮箱注册请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmailRegisterRequest extends BaseRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Parameter(description = "邮箱地址", required = true)
    private String email;

    @NotNull(message = "验证码id不能为空")
    @Parameter(description = "验证码id", required = true)
    private Long verifyId;

    @NotBlank(message = "验证码不能为空")
    @Parameter(description = "验证码", required = true)
    private String code;

}
