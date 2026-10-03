package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查看用户资料请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserIdReq extends BaseRequest {

    @Parameter(description = "目标用户ID", required = true)
    private Long userId;
}
