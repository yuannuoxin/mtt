package com.mtd.st.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mtd.common.core.result.Result;
import com.mtd.st.constant.PermCode;
import com.mtd.st.model.entity.SysUser;
import com.mtd.st.model.req.*;
import com.mtd.st.service.UserService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@SaCheckLogin
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户列表")
    @PostMapping("/page")
    @SaCheckPermission(PermCode.USER_VIEW)
    public Result<Page<SysUser>> page(@RequestBody UserPageReq req) {
        return Result.success(userService.page(req));
    }

    @Operation(summary = "用户详情")
    @PostMapping("/getById")
    @SaCheckPermission(PermCode.USER_VIEW)
    public Result<SysUser> getById(@RequestBody IdReq req) {
        return Result.success(userService.getById(req.getId()));
    }

    @Operation(summary = "创建用户")
    @PostMapping("/create")
    @SaCheckPermission(PermCode.USER_CREATE)
    public Result<Void> create(@RequestBody UserCreateReq req) {
        userService.create(req);
        return Result.success();
    }

    @Operation(summary = "更新用户")
    @PostMapping("/update")
    @SaCheckPermission(PermCode.USER_UPDATE)
    public Result<Void> update(@RequestBody UserUpdateReq req) {
        userService.update(req);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @PostMapping("/delete")
    @SaCheckPermission(PermCode.USER_DELETE)
    public Result<Void> delete(@RequestBody IdReq req) {
        userService.delete(req.getId());
        return Result.success();
    }

    @Operation(summary = "分配角色")
    @PostMapping("/assignRoles")
    @SaCheckPermission(PermCode.USER_ASSIGN_ROLE)
    public Result<Void> assignRoles(@RequestBody AssignRoleReq req) {
        userService.assignRoles(req);
        return Result.success();
    }
}
