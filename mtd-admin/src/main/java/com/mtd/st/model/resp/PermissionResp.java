package com.mtd.st.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.util.List;

@Data
public class PermissionResp {

    @Parameter(description = "角色编码列表")
    private List<String> roleCodes;

    @Parameter(description = "权限编码列表")
    private List<String> permissionCodes;
}
