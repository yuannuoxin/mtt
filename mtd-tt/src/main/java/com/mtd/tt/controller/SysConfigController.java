package com.mtd.tt.controller;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.model.entity.UserEntity;
import com.mtd.tt.model.req.SetConfigReq;
import com.mtd.tt.service.SysConfigService;
import com.mtd.tt.util.LoginUtil;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * 系统控制配置接口：运行时开关 Turnstile、邮箱验证码等能力，改动即时生效。
 */
@Tag(name = "系统控制配置", description = "运行时开关 Turnstile / 邮箱验证码等能力")
@RestController
@RequestMapping("/sysConfig")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;
    private final UserMapper userMapper;

    /** 允许通过接口修改的配置键白名单（不含任何越权键） */
    private static final Set<String> ALLOWED_KEYS = Set.of(
            "turnstile.enabled", "turnstile.mode",
            "turnstile.site_key", "turnstile.secret_key",
            "turnstile.managed.site_key", "turnstile.managed.secret_key",
            "turnstile.invisible.site_key", "turnstile.invisible.secret_key",
            "captcha.provider",
            "recaptcha.mode",
            "recaptcha.site_key", "recaptcha.secret_key", "recaptcha.threshold",
            "recaptcha.v2.site_key", "recaptcha.v2.secret_key",
            "recaptcha.v2_invisible.site_key", "recaptcha.v2_invisible.secret_key",
            "email_code.enabled",
            "email.smtp.host", "email.smtp.port", "email.smtp.username",
            "email.smtp.password", "email.smtp.from",
            "email.domain.blacklist",
            "points.checkin", "points.post", "points.reply",
            "points.upload_per_mb", "points.initial",
            "exp.per.post", "exp.per.quote", "exp.per.repost", "exp.per.reply",
            "exp.level.thresholds",
            "oauth.google.enabled", "oauth.google.client_id",
            "oauth.google.client_secret", "oauth.google.redirect_uri", "oauth.google.mode",
            "oauth.github.enabled", "oauth.github.client_id",
            "oauth.github.client_secret", "oauth.github.redirect_uri",
            "oauth.telegram.enabled", "oauth.telegram.client_id",
            "oauth.telegram.client_secret", "oauth.telegram.redirect_uri",
            "oauth.proxy.enabled", "oauth.proxy.host", "oauth.proxy.port");

    @Operation(summary = "更新控制配置（仅管理员）")
    @PostMapping("/set")
    public Result<Map<String, String>> set(@RequestBody SetConfigReq req) {
        requireAdmin();
        if (req.getKey() == null || req.getKey().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "配置键不能为空");
        }
        if (!ALLOWED_KEYS.contains(req.getKey())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不允许修改该配置项");
        }
        sysConfigService.set(req.getKey(), req.getValue());
        return Result.success(Map.of("key", req.getKey(), "value", req.getValue() == null ? "" : req.getValue()));
    }

    /** 仅允许管理员调用：当前登录用户 is_admin 为 true 才放行，否则拒绝 */
    private void requireAdmin() {
        Long userId = LoginUtil.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserEntity::getId, userId));
        if (user == null || !Boolean.TRUE.equals(user.getAdmin())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权限，仅管理员可修改系统配置");
        }
    }
}
