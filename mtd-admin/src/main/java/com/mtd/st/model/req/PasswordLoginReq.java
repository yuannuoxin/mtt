package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordLoginReq extends BaseRequest {

    @NotBlank(message = "账号不能为空")
    @Parameter(description = "账号(用户名/邮箱/手机号)", required = true)
    private String account;

    @NotBlank(message = "密码不能为空")
    @Parameter(description = "密码", required = true)
    private String password;
}
