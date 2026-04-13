package com.mtd.st.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mtd.common.core.result.Result;
import com.mtd.st.model.req.EmailCodeLoginReq;
import com.mtd.st.model.req.PasswordLoginReq;
import com.mtd.st.model.req.SendEmailCodeReq;
import com.mtd.st.model.resp.LoginResp;
import com.mtd.st.model.resp.PermissionResp;
import com.mtd.st.model.resp.UserInfoResp;
import com.mtd.st.service.AuthService;
import com.mtd.st.utils.MapstructUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证管理", description = "登录/登出接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.mtd.st.mapper.SysUserMapper userMapper;

    @Operation(summary = "账号密码登录")
    @PostMapping("/passwordLogin")
    public Result<LoginResp> passwordLogin(@RequestBody PasswordLoginReq req) {
        return Result.success(authService.passwordLogin(req));
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/emailCodeLogin")
    public Result<LoginResp> emailCodeLogin(@RequestBody EmailCodeLoginReq req) {
        return Result.success(authService.emailCodeLogin(req));
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/sendEmailCode")
    public Result<Void> sendEmailCode(@RequestBody SendEmailCodeReq req) {
        authService.sendEmailCode(req.getEmail());
        return Result.success();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @PostMapping("/userInfo")
    @SaCheckLogin
    public Result<UserInfoResp> userInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        var user = userMapper.selectOneById(userId);
        UserInfoResp resp = MapstructUtils.convert(user, UserInfoResp.class);

        return Result.success(resp);
    }

    @Operation(summary = "获取当前用户权限信息")
    @PostMapping("/getPermissions")
    @SaCheckLogin
    public Result<PermissionResp> getPermissions() {
        return authService.getPermissions();
    }


}
