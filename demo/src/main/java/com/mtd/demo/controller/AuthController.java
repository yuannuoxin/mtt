package com.mtd.demo.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtd.common.core.constant.CommonConstants;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.demo.entity.User;
import com.mtd.demo.request.LoginRequest;
import com.mtd.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 - Sa-Token 示例
 */
@Tag(name = "认证管理", description = "用户登录/登出接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户登录", description = "用户名密码登录，返回 Token 信息")
    @PostMapping("/login")
    public Result<SaTokenInfo> login(@RequestBody LoginRequest request) {
        // 1. 参数校验
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "密码不能为空");
        }

        // 2. 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userService.getOne(wrapper);

        // 3. 验证用户是否存在
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_ERROR.getCode(), "用户不存在");
        }

        // 4. 验证用户状态
        if (CommonConstants.UserStatus.DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户已被禁用，请联系管理员");
        }

        // 5. 验证密码（MD5 加密）
        String encryptedPassword = SecureUtil.md5(request.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        // 6. 登录成功，生成 Token
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        return Result.success(tokenInfo);
    }

    @Operation(summary = "用户登出", description = "退出登录，清除 Token")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前登录信息", description = "获取当前登录用户的 ID")
    @GetMapping("/info")
    public Result<Object> getInfo() {
        if (!StpUtil.isLogin()) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        return Result.success(StpUtil.getLoginId());
    }
}
