package com.mtd.st.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mtd.common.core.result.Result;
import com.mtd.st.model.entity.SysPermission;
import com.mtd.st.constant.PermCode;
import com.mtd.st.model.req.IdReq;
import com.mtd.st.model.req.PermissionCreateReq;
import com.mtd.st.model.req.PermissionPageReq;
import com.mtd.st.model.req.PermissionUpdateReq;
import com.mtd.st.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.mybatisflex.core.paginate.Page;
import java.util.List;

@Tag(name = "权限管理")
@RestController
@RequestMapping("/system/permission")
@RequiredArgsConstructor
@SaCheckLogin
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "权限列表")
    @PostMapping("/page")
    @SaCheckPermission(PermCode.PERM_VIEW)
    public Result<Page<SysPermission>> page(@RequestBody PermissionPageReq req) {
        return Result.success(permissionService.page(req));
    }

    @Operation(summary = "权限详情")
    @PostMapping("/getById")
    @SaCheckPermission(PermCode.PERM_VIEW)
    public Result<SysPermission> getById(@RequestBody IdReq req) {
        return Result.success(permissionService.getById(req.getId()));
    }

    @Operation(summary = "根据父级ID查询")
    @PostMapping("/listByParentId")
    @SaCheckPermission(PermCode.PERM_VIEW)
    public Result<List<SysPermission>> listByParentId(@RequestParam(required = false) Long parentId) {
        return Result.success(permissionService.listByParentId(parentId));
    }

    @Operation(summary = "创建权限")
    @PostMapping("/create")
    @SaCheckPermission(PermCode.PERM_CREATE)
    public Result<Void> create(@RequestBody PermissionCreateReq req) {
        permissionService.create(req);
        return Result.success();
    }

    @Operation(summary = "更新权限")
    @PostMapping("/update")
    @SaCheckPermission(PermCode.PERM_UPDATE)
    public Result<Void> update(@RequestBody PermissionUpdateReq req) {
        permissionService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除权限")
    @PostMapping("/delete")
    @SaCheckPermission(PermCode.PERM_DELETE)
    public Result<Void> delete(@RequestBody IdReq req) {
        permissionService.delete(req.getId());
        return Result.success();
    }
}
