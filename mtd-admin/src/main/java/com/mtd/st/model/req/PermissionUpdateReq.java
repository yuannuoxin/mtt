package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionUpdateReq extends BaseRequest {

    @NotNull(message = "权限ID不能为空")
    @Parameter(description = "权限ID", required = true)
    private Long id;

    @Parameter(description = "权限名称")
    private String permName;

    @Parameter(description = "路由路径")
    private String path;

    @Parameter(description = "组件路径")
    private String component;

    @Parameter(description = "图标")
    private String icon;

    @Parameter(description = "排序")
    private Integer sort;

    @Parameter(description = "状态: 0禁用 1正常")
    private Integer status;
}
