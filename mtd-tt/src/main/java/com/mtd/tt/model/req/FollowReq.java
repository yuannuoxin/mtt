package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关注请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FollowReq extends BaseRequest {

    @Parameter(description = "被关注用户ID", required = true)
    private Long followeeId;
}
