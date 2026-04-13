package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionCreateReq extends BaseRequest {

    @Parameter(description = "父级ID")
    private Long parentId;

    @NotBlank(message = "权限编码不能为空")
    @Parameter(description = "权限编码", required = true)
    private String permCode;

    @NotBlank(message = "权限名称不能为空")
    @Parameter(description = "权限名称", required = true)
    private String permName;

    @NotNull(message = "权限类型不能为空")
    @Parameter(description = "类型: 1菜单 2按钮 3接口", required = true)
    private Integer permType;

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
