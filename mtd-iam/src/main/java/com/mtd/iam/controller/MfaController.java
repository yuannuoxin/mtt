package com.mtd.iam.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.mtd.common.core.result.Result;
import com.mtd.iam.entity.UserMfa;
import com.mtd.iam.enums.YesEnum;
import com.mtd.iam.mapper.UserMfaMapper;
import com.mtd.iam.req.MfaManageRequest;
import com.mtd.iam.resp.MfaInfoResp;
import com.mtd.iam.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MFA 管理控制器
 */
@Slf4j
@Tag(name = "MFA 管理")
@RestController
@RequestMapping("/mfa")
public class MfaController {

    @Autowired
    private MfaService mfaService;
    @Autowired
    private UserMfaMapper userMfaMapper;

    /**
     * 获取当前用户的 MFA 信息
     */
    @Operation(summary = "获取 MFA 信息", description = "获取当前登录用户的 MFA 配置信息")
    @PostMapping("/info")
    @SaCheckLogin
    public Result<MfaInfoResp> getMfaInfo() {
        Long userId = getCurrentUserId();
        UserMfa userMfa = mfaService.getUserMfa(userId);

        if (ObjUtil.isNull(userMfa)) {
            // 用户还没有 MFA 配置
            MfaInfoResp resp = new MfaInfoResp();
            resp.setUserId(userId);
            resp.setEnabled(false);
            return Result.success(resp);
        }

        MfaInfoResp resp = new MfaInfoResp();
        resp.setId(userMfa.getId());
        resp.setUserId(userId);
        resp.setEnabled(YesEnum.YES.getCode().equals(userMfa.getStatus()));
        return Result.success(resp);
    }

    /**
     * 添加 MFA
     */
    @Operation(summary = "添加 MFA", description = "为用户生成 MFA 配置（密钥和二维码），用于绑定身份验证器")
    @PostMapping("/add")
    @SaCheckLogin
    public Result<MfaInfoResp> addMfa(@RequestBody MfaManageRequest req) {
        Long userId = getCurrentUserId();
        log.info("添加 MFA，userId: {}", userId);

        // 添加或获取 MFA 配置
        UserMfa userMfa = mfaService.addMfa(userId);

        MfaInfoResp resp = new MfaInfoResp();
        resp.setId(userMfa.getId());
        resp.setUserId(userId);
        resp.setSecret(userMfa.getSecret());
        resp.setEnabled(YesEnum.YES.getCode().equals(userMfa.getStatus()));

        // 生成二维码 URL（可以使用 Google Chart API 或其他二维码服务）
        // 格式：otpauth://totp/Issuer:username?secret=SECRET&issuer=Issuer
        String otpAuthUrl = String.format("otpauth://totp/MTD:user_%d?secret=%s&issuer=MTD",
                userId, userMfa.getSecret());
        resp.setQrCodeUrl(otpAuthUrl);

        log.info("MFA 添加成功，userId: {}, mfaId: {}", userId, userMfa.getId());
        return Result.success(resp);
    }

    /**
     * 更新 MFA 状态
     */
    @Operation(summary = "更新 MFA 状态", description = "启用或禁用 MFA（需要提供正确的验证码）")
    @PostMapping("/update")
    @SaCheckLogin
    public Result<Void> updateMfa(@RequestBody MfaManageRequest req) {
        Long userId = getCurrentUserId();
        log.info("更新 MFA，userId: {}, mfaId: {}", userId, req.getMfaId());

        // 验证 MFA 是否属于当前用户
        UserMfa userMfa = mfaService.getUserMfa(userId);
        if (ObjUtil.isNull(userMfa) || !userMfa.getId().equals(req.getMfaId())) {
            log.warn("MFA 配置不存在或不属于当前用户，userId: {}, mfaId: {}", userId, req.getMfaId());
            return Result.error("MFA 配置不存在");
        }

        // 如果修改状态，需要验证当前验证码
        if (req.getStatus() != null && YesEnum.YES.getCode().equals(req.getStatus())) {
            if (StrUtil.isEmpty(req.getCode())) {
                log.warn("启用 MFA 需要提供验证码，userId: {}", userId);
                return Result.error("请提供当前 MFA 验证码以确认身份");
            }

            boolean verified = mfaService.verifyMfaCode(req.getMfaId(), req.getCode());
            if (!verified) {
                log.warn("MFA 验证码错误，userId: {}", userId);
                return Result.error("验证码错误");
            }
        }

        // 更新状态
        if (req.getStatus() != null) {
            mfaService.updateMfaStatus(req.getMfaId(), req.getStatus());
        }

        log.info("MFA 更新成功，userId: {}, status: {}", userId, req.getStatus());
        return Result.success();
    }

    /**
     * 删除 MFA
     */
    @Operation(summary = "删除 MFA", description = "删除用户的 MFA 配置（需要提供正确的验证码）")
    @PostMapping("/delete")
    @SaCheckLogin
    public Result<Void> deleteMfa(@RequestBody MfaManageRequest req) {
        Long userId = getCurrentUserId();
        log.info("删除 MFA，userId: {}, mfaId: {}", userId, req.getMfaId());

        // 验证 MFA 是否属于当前用户
        UserMfa userMfa = mfaService.getUserMfa(userId);
        if (ObjUtil.isNull(userMfa) || !userMfa.getId().equals(req.getMfaId())) {
            log.warn("MFA 配置不存在或不属于当前用户，userId: {}, mfaId: {}", userId, req.getMfaId());
            return Result.error("MFA 配置不存在");
        }

        // 删除前必须验证验证码
        if (StrUtil.isEmpty(req.getCode())) {
            log.warn("删除 MFA 需要提供验证码，userId: {}", userId);
            return Result.error("请提供当前 MFA 验证码以确认身份");
        }

        boolean verified = mfaService.verifyMfaCode(req.getMfaId(), req.getCode());
        if (!verified) {
            log.warn("MFA 验证码错误，userId: {}", userId);
            return Result.error("验证码错误");
        }


        userMfaMapper.deleteById(req.getMfaId());

        log.info("MFA 删除成功，userId: {}", userId);
        return Result.success();
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
