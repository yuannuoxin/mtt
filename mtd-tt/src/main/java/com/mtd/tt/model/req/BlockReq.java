package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 拉黑请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BlockReq extends BaseRequest {

    @Parameter(description = "被拉黑用户ID", required = true)
    private Long blockedUserId;
}
