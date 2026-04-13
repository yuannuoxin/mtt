package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmailCodeLoginReq extends BaseRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Parameter(description = "邮箱", required = true)
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Parameter(description = "验证码", required = true)
    private String code;
}
