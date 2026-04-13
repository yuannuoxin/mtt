package com.mtd.st.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionPageReq extends PageReq {

    @Parameter(description = "权限编码")
    private String permCode;

    @Parameter(description = "权限名称")
    private String permName;

    @Parameter(description = "类型: 1菜单 2按钮 3接口")
    private Integer permType;

    @Parameter(description = "状态: 0禁用 1正常")
    private Integer status;
}
