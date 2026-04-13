package com.mtd.st.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mtd.common.core.result.Result;
import com.mtd.st.model.entity.SysRole;
import com.mtd.st.constant.PermCode;
import com.mtd.st.model.req.AssignPermissionReq;
import com.mtd.st.model.req.IdReq;
import com.mtd.st.model.req.RoleCreateReq;
import com.mtd.st.model.req.RolePageReq;
import com.mtd.st.model.req.RoleUpdateReq;
import com.mtd.st.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.mybatisflex.core.paginate.Page;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
@SaCheckLogin
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表")
    @PostMapping("/page")
    @SaCheckPermission(PermCode.ROLE_VIEW)
    public Result<Page<SysRole>> page(@RequestBody RolePageReq req) {
        return Result.success(roleService.page(req));
    }

    @Operation(summary = "角色详情")
    @PostMapping("/getById")
    @SaCheckPermission(PermCode.ROLE_VIEW)
    public Result<SysRole> getById(@RequestBody IdReq req) {
        return Result.success(roleService.getById(req.getId()));
    }

    @Operation(summary = "创建角色")
    @PostMapping("/create")
    @SaCheckPermission(PermCode.ROLE_CREATE)
    public Result<Void> create(@RequestBody RoleCreateReq req) {
        roleService.create(req);
        return Result.success();
    }

    @Operation(summary = "更新角色")
    @PostMapping("/update")
    @SaCheckPermission(PermCode.ROLE_UPDATE)
    public Result<Void> update(@RequestBody RoleUpdateReq req) {
        roleService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @PostMapping("/delete")
    @SaCheckPermission(PermCode.ROLE_DELETE)
    public Result<Void> delete(@RequestBody IdReq req) {
        roleService.delete(req.getId());
        return Result.success();
    }

    @Operation(summary = "分配权限")
    @PostMapping("/assignPermissions")
    @SaCheckPermission(PermCode.ROLE_ASSIGN_PERM)
    public Result<Void> assignPermissions(@RequestBody AssignPermissionReq req) {
        roleService.assignPermissions(req);
        return Result.success();
    }
}
