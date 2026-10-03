package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 注册请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterReq extends BaseRequest {

    @Parameter(description = "用户名", required = true)
    private String username;

    @Parameter(description = "昵称", required = true)
    private String nickname;

    @Parameter(description = "邮箱", required = true)
    private String email;

    @Parameter(description = "密码", required = true)
    private String password;
}
