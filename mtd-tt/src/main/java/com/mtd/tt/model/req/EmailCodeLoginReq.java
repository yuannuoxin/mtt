package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邮箱验证码登录请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmailCodeLoginReq extends BaseRequest {

    @Parameter(description = "邮箱", required = true)
    private String email;

    @Parameter(description = "验证码", required = true)
    private String code;
}
