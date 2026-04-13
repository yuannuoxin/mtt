package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AssignRoleReq extends BaseRequest {

    @NotNull(message = "用户ID不能为空")
    @Parameter(description = "用户ID", required = true)
    private Long userId;

    @NotNull(message = "角色ID列表不能为空")
    @Parameter(description = "角色ID列表", required = true)
    private java.util.List<Long> roleIds;
}
