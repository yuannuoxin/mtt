package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发送邮箱验证码请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SendEmailCodeReq extends BaseRequest {

    @Parameter(description = "邮箱", required = true)
    private String email;
}
