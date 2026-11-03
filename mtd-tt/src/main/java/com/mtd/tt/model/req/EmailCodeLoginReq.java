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

    @Parameter(description = "Turnstile 人机校验 token（启用时必填）")
    private String turnstileToken;

    @Parameter(description = "浏览器指纹（FingerprintJS v5 MIT，登录时记录）")
    private String fingerprint;
}
