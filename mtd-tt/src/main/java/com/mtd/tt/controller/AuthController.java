package com.mtd.tt.controller;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.model.req.EmailCodeLoginReq;
import com.mtd.tt.model.req.ForgotPasswordReq;
import com.mtd.tt.model.req.KickDeviceReq;
import com.mtd.tt.model.req.PasswordLoginReq;
import com.mtd.tt.model.req.RegisterReq;
import com.mtd.tt.model.req.SendEmailCodeReq;
import com.mtd.tt.model.resp.DeviceResp;
import com.mtd.tt.model.resp.LoginResp;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mtd.tt.service.AuthService;
import com.mtd.tt.service.DeviceService;
import com.mtd.tt.service.TurnstileService;
import com.mtd.tt.util.LoginUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 认证接口：注册 / 登录 / 登出 / 当前用户
 */
@Tag(name = "认证", description = "注册、登录、登出、当前用户")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TurnstileService turnstileService;
    private final DeviceService deviceService;

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
        authService.sendEmailCode(req);
        return Result.success();
    }

    @Operation(summary = "Turnstile 前端配置（是否启用 + siteKey + 邮箱验证码开关）")
    @PostMapping("/turnstileConfig")
    public Result<Map<String, Object>> turnstileConfig() {
        return Result.success(turnstileService.publicConfig());
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/emailCodeLogin")
    public Result<LoginResp> emailCodeLogin(@RequestBody EmailCodeLoginReq req) {
        return Result.success(authService.emailCodeLogin(req));
    }

    @Operation(summary = "忘记密码（邮箱验证码重置密码）")
    @PostMapping("/forgotPassword")
    public Result<Void> forgotPassword(@RequestBody ForgotPasswordReq req) {
        authService.forgotPassword(req);
        return Result.success();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息")
    @PostMapping("/userInfo")
    public Result<UserProfileResp> userInfo() {
        return Result.success(authService.currentUserInfo());
    }

    @Operation(summary = "当前用户登录记录（分页，含浏览器指纹/IP/UA）")
    @PostMapping("/loginLogs")
    public Result<com.mtd.common.core.response.PageResponse<com.mtd.tt.model.resp.LoginLogResp>> loginLogs(
            @RequestBody(required = false) com.mtd.common.core.request.PageRequest page) {
        return Result.success(authService.loginLogs(page));
    }

    @Operation(summary = "当前账号在线登录设备列表")
    @PostMapping("/devices")
    public Result<List<DeviceResp>> devices() {
        return Result.success(deviceService.list(requireLogin()));
    }

    @Operation(summary = "下线指定登录设备")
    @PostMapping("/kickDevice")
    public Result<Map<String, Object>> kickDevice(@RequestBody KickDeviceReq req) {
        boolean current = deviceService.kick(requireLogin(), req.getDeviceId());
        return Result.success(Map.of("current", current));
    }

    @Operation(summary = "下线其他所有登录设备")
    @PostMapping("/kickOtherDevices")
    public Result<Map<String, Object>> kickOtherDevices() {
        return Result.success(Map.of("count", deviceService.kickOthers(requireLogin())));
    }

    private Long requireLogin() {
        Long userId = LoginUtil.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        return userId;
    }
}
