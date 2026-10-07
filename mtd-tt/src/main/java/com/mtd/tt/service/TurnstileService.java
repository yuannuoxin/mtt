package com.mtd.tt.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 人机校验服务，支持两种提供商（由 captcha.provider 控制，非法值回退 turnstile）：
 * <ul>
 *   <li>turnstile ：Cloudflare Turnstile（managed / invisible / non_interactive 三种模式）</li>
 *   <li>recaptcha ：Google reCAPTCHA（v3 无感评分 / v2 复选框 / v2_invisible 隐形三种模式，
 *       统一走 recaptcha.net 域名，便于大陆访问）</li>
 * </ul>
 * 是否启用由 sys_config 的 turnstile.enabled 统一控制：开发期默认关闭则校验直接放行，
 * 关闭/开启实时生效，无需重启。
 */
@Slf4j
@Service
public class TurnstileService {

    private static final String TURNSTILE_SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final String RECAPTCHA_SITEVERIFY_URL = "https://www.recaptcha.net/recaptcha/api/siteverify";
    private static final int HTTP_TIMEOUT_MS = 8000;

    @Autowired
    private SysConfigService configService;

    public boolean isEnabled() {
        return configService.getBool("turnstile.enabled", false);
    }

    /** 当前人机校验提供商：recaptcha / turnstile（非法值回退 turnstile） */
    public String provider() {
        String p = StrUtil.nullToEmpty(configService.get("captcha.provider"));
        return "recaptcha".equalsIgnoreCase(p) ? "recaptcha" : "turnstile";
    }

    /** 当前 Turnstile 模式：managed / invisible / non_interactive（非法值回退 non_interactive） */
    public String mode() {
        String m = StrUtil.nullToEmpty(configService.get("turnstile.mode"));
        return switch (m) {
            case "managed", "invisible" -> m;
            default -> "non_interactive";
        };
    }

    /** 当前 reCAPTCHA 模式：v3 / v2 / v2_invisible（非法值回退 v3） */
    public String recaptchaMode() {
        String m = StrUtil.nullToEmpty(configService.get("recaptcha.mode"));
        return switch (m) {
            case "v2", "v2_invisible" -> m;
            default -> "v3";
        };
    }

    private String firstNonBlank(String a, String b) {
        return StrUtil.isNotBlank(a) ? a : StrUtil.nullToEmpty(b);
    }

    /** 按提供商取 Site Key（前端渲染用） */
    public String siteKey() {
        if ("recaptcha".equals(provider())) {
            return switch (recaptchaMode()) {
                case "v2" -> firstNonBlank(configService.get("recaptcha.v2.site_key"), configService.get("recaptcha.site_key"));
                case "v2_invisible" -> firstNonBlank(configService.get("recaptcha.v2_invisible.site_key"), configService.get("recaptcha.site_key"));
                default -> StrUtil.nullToEmpty(configService.get("recaptcha.site_key"));
            };
        }
        String key = switch (mode()) {
            case "managed" -> configService.get("turnstile.managed.site_key");
            case "invisible" -> configService.get("turnstile.invisible.site_key");
            default -> configService.get("turnstile.site_key");
        };
        if (StrUtil.isBlank(key)) {
            key = configService.get("turnstile.site_key");
        }
        return StrUtil.nullToEmpty(key);
    }

    /** 按提供商取 Secret Key（后端 siteverify 用） */
    public String secretKey() {
        if ("recaptcha".equals(provider())) {
            return switch (recaptchaMode()) {
                case "v2" -> firstNonBlank(configService.get("recaptcha.v2.secret_key"), configService.get("recaptcha.secret_key"));
                case "v2_invisible" -> firstNonBlank(configService.get("recaptcha.v2_invisible.secret_key"), configService.get("recaptcha.secret_key"));
                default -> StrUtil.nullToEmpty(configService.get("recaptcha.secret_key"));
            };
        }
        String key = switch (mode()) {
            case "managed" -> configService.get("turnstile.managed.secret_key");
            case "invisible" -> configService.get("turnstile.invisible.secret_key");
            default -> configService.get("turnstile.secret_key");
        };
        if (StrUtil.isBlank(key)) {
            key = configService.get("turnstile.secret_key");
        }
        return StrUtil.nullToEmpty(key);
    }

    /**
     * 校验人机 token；未启用时直接放行（开发便利）。
     * 按当前 provider 分流到 Turnstile / reCAPTCHA。
     */
    public void verify(String token) {
        if (!isEnabled()) {
            return;
        }
        if ("recaptcha".equals(provider())) {
            verifyRecaptcha(token);
        } else {
            verifyTurnstile(token);
        }
    }

    private void verifyTurnstile(String token) {
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "人机校验未通过，请刷新后重试");
        }
        String secret = secretKey();
        Map<String, Object> body = new HashMap<>();
        body.put("secret", secret == null ? "" : secret);
        body.put("response", token);
        try {
            String resp = HttpUtil.post(TURNSTILE_SITEVERIFY_URL, body, HTTP_TIMEOUT_MS);
            JSONObject json = JSONUtil.parseObj(resp);
            if (!json.getBool("success", false)) {
                log.warn("Turnstile 校验失败: {}", resp);
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "人机校验未通过，请刷新后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Turnstile siteverify 调用异常: {}", e.getMessage(), e);
            // 校验服务异常时不阻断登录，避免外部依赖导致功能不可用
            log.warn("Turnstile 服务异常，本次放行");
        }
    }

    private void verifyRecaptcha(String token) {
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "人机校验未通过，请刷新后重试");
        }
        String secret = secretKey();
        Map<String, Object> body = new HashMap<>();
        body.put("secret", secret == null ? "" : secret);
        body.put("response", token);
        try {
            String resp = HttpUtil.post(RECAPTCHA_SITEVERIFY_URL, body, HTTP_TIMEOUT_MS);
            JSONObject json = JSONUtil.parseObj(resp);
            if (!json.getBool("success", false)) {
                log.warn("reCAPTCHA 校验失败: {}", resp);
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "人机校验未通过，请刷新后重试");
            }
            // v3 才校验 score 阈值；v2 / v2_invisible 无分数，只需 success
            if ("v3".equals(recaptchaMode())) {
                double score = json.getDouble("score", 0.0);
                double threshold = parseThreshold();
                if (score < threshold) {
                    log.warn("reCAPTCHA v3 score={} 低于阈值 {}，拒绝", score, threshold);
                    throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "人机校验未通过，请刷新后重试");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("reCAPTCHA siteverify 调用异常: {}", e.getMessage(), e);
            // 校验服务异常时不阻断登录，避免外部依赖导致功能不可用
            log.warn("reCAPTCHA 服务异常，本次放行");
        }
    }

    private double parseThreshold() {
        String v = configService.get("recaptcha.threshold");
        if (StrUtil.isBlank(v)) {
            return 0.5;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }

    /**
     * 前端渲染所需的公开配置（只暴露 siteKey，不暴露 secret）
     */
    public Map<String, Object> publicConfig() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("enabled", isEnabled());
        cfg.put("provider", provider());
        cfg.put("mode", mode());
        cfg.put("recaptchaMode", recaptchaMode());
        cfg.put("siteKey", siteKey());
        cfg.put("emailCodeEnabled", configService.getBool("email_code.enabled", false));
        return cfg;
    }
}
