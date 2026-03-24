package com.mtd.iam.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.SaTokenInfo;
import com.mtd.common.core.result.Result;
import com.mtd.iam.req.PasskeyLoginRequest;
import com.mtd.iam.req.PasskeyRegisterRequest;
import com.mtd.iam.req.PasskeyChallengeRequest;
import com.mtd.iam.req.DeletePasskeyRequest;
import com.mtd.iam.resp.PasskeyChallengeResp;
import com.mtd.iam.resp.PasskeyListResp;
import com.mtd.iam.service.PasskeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Passkey 认证控制器
 */
@Slf4j
@Tag(name = "Passkey 认证", description = "Passkey 注册、登录相关接口")
@RestController
@RequestMapping("/auth/passkey")
public class PasskeyController {

    @Autowired
    private PasskeyService passkeyService;

    /**
     * 生成 Passkey 注册挑战值
     */
    @Operation(summary = "生成注册挑战值", description = "生成用于 Passkey 注册的挑战值")
    @PostMapping("/register/challenge")
    @SaCheckLogin
    public Result<PasskeyChallengeResp> generateRegisterChallenge(@RequestBody PasskeyChallengeRequest req) {
        return Result.success(passkeyService.generateRegisterChallenge(req));
    }

    /**
     * 完成 Passkey 注册
     */
    @Operation(summary = "完成 Passkey 注册", description = "验证并保存 Passkey 信息")
    @PostMapping("/register/complete")
    @SaCheckLogin
    public Result<Void> completeRegister(@RequestBody PasskeyRegisterRequest req) {
        log.info("收到 Passkey 注册请求，credentialId: {}", req.getCredentialId());
        passkeyService.completeRegister(req);
        return Result.success();
    }

    /**
     * 生成 Passkey 登录挑战值
     */
    @Operation(summary = "生成登录挑战值", description = "生成用于 Passkey 登录的挑战值")
    @PostMapping("/login/challenge")
    @SaIgnore
    public Result<PasskeyChallengeResp> generateLoginChallenge(@RequestBody PasskeyChallengeRequest req) {
        return Result.success(passkeyService.generateLoginChallenge(req));
    }

    /**
     * 完成 Passkey 登录
     */
    @Operation(summary = "完成 Passkey 登录", description = "验证 Passkey 签名并登录")
    @PostMapping("/login/complete")
    @SaIgnore
    public Result<SaTokenInfo> completeLogin(@RequestBody PasskeyLoginRequest req) {
        log.info("收到 Passkey 登录请求，credentialId: {}", req.getCredentialId());
        return Result.success(passkeyService.completeLogin(req));
    }

    /**
     * 获取当前用户的 Passkey 列表
     */
    @Operation(summary = "获取 Passkey 列表", description = "获取当前用户已注册的 Passkey 设备列表")
    @PostMapping("/list")
    @SaCheckLogin
    public Result<PasskeyListResp> getUserPasskeys() {
        return Result.success(passkeyService.getUserPasskeys());
    }

    /**
     * 删除 Passkey
     */
    @Operation(summary = "删除 Passkey", description = "删除指定的 Passkey 设备")
    @PostMapping("/delete")
    @SaCheckLogin
    public Result<Void> deletePasskey(@RequestBody DeletePasskeyRequest req) {
        log.info("收到删除 Passkey 请求，credentialId: {}", req.getCredentialId());
        passkeyService.deletePasskey(req.getCredentialId());
        return Result.success();
    }
}
