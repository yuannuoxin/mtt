package com.mtd.st.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RolePageReq extends PageReq {

    @Parameter(description = "角色编码")
    private String roleCode;

    @Parameter(description = "角色名称")
    private String roleName;

    @Parameter(description = "状态: 0禁用 1正常")
    private Integer status;
}
