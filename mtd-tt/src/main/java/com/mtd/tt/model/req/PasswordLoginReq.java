package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密码登录请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordLoginReq extends BaseRequest {

    @Parameter(description = "用户名或邮箱", required = true)
    private String account;

    @Parameter(description = "密码", required = true)
    private String password;
}
