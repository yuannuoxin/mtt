package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleUpdateReq extends BaseRequest {

    @NotNull(message = "角色ID不能为空")
    @Parameter(description = "角色ID", required = true)
    private Long id;

    @Parameter(description = "角色名称")
    private String roleName;

    @Parameter(description = "描述")
    private String description;

    @Parameter(description = "排序")
    private Integer sort;

    @Parameter(description = "状态: 0禁用 1正常")
    private Integer status;
}
