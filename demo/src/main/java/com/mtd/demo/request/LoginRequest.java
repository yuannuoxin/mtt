package com.mtd.demo.request;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户登录请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginRequest extends BaseRequest {

    @Parameter(description = "用户名", required = true)
    private String username;

    @Parameter(description = "密码", required = true)
    private String password;
}
