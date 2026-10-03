package com.mtd.tt.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mtd.common.core.result.Result;
import com.mtd.tt.model.req.EmailCodeLoginReq;
import com.mtd.tt.model.req.PasswordLoginReq;
import com.mtd.tt.model.req.RegisterReq;
import com.mtd.tt.model.req.SendEmailCodeReq;
import com.mtd.tt.model.resp.LoginResp;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mtd.tt.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 登出 / 当前用户
 */
@Tag(name = "认证", description = "注册、登录、登出、当前用户")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "账号密码登录")
    @PostMapping("/passwordLogin")
    public Result<LoginResp> passwordLogin(@RequestBody PasswordLoginReq req) {
        return Result.success(authService.passwordLogin(req));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginResp> register(@RequestBody RegisterReq req) {
        return Result.success(authService.register(req));
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/sendEmailCode")
    public Result<Void> sendEmailCode(@RequestBody SendEmailCodeReq req) {
        authService.sendEmailCode(req.getEmail());
        return Result.success();
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/emailCodeLogin")
    public Result<LoginResp> emailCodeLogin(@RequestBody EmailCodeLoginReq req) {
        return Result.success(authService.emailCodeLogin(req));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息")
    @PostMapping("/userInfo")
    @SaCheckLogin
    public Result<UserProfileResp> userInfo() {
        return Result.success(authService.currentUserInfo());
    }
}
