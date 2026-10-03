package com.mtd.tt.model.req;

import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 某用户的粉丝/关注列表请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserListReq extends PageRequest {

    @Parameter(description = "目标用户ID", required = true)
    private Long userId;
}
