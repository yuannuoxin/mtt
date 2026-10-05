package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新系统控制配置请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SetConfigReq extends BaseRequest {

    @Parameter(description = "配置键（如 turnstile.enabled / email_code.enabled）", required = true)
    private String key;

    @Parameter(description = "配置值")
    private String value;
}
