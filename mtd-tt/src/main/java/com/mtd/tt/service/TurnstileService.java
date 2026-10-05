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
 * Cloudflare Turnstile 人机校验（Non-Interactive 非交互模式）。
 * <p>是否启用由 sys_config 的 turnstile.enabled 控制：开发期默认关闭则校验直接放行，
 * 关闭/开启实时生效，无需重启。</p>
 */
@Slf4j
@Service
public class TurnstileService {

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final int HTTP_TIMEOUT_MS = 8000;

    @Autowired
    private SysConfigService configService;

    public boolean isEnabled() {
        return configService.getBool("turnstile.enabled", false);
    }

    /** 当前 Turnstile 模式：managed / invisible / non_interactive（非法值回退 non_interactive） */
    public String mode() {
        String m = StrUtil.nullToEmpty(configService.get("turnstile.mode"));
        return switch (m) {
            case "managed", "invisible" -> m;
            default -> "non_interactive";
        };
    }

    /** 按当前模式取 Site Key（前端渲染用），未配置则回退 turnstile.site_key */
    public String siteKey() {
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

    /** 按当前模式取 Secret Key（后端 siteverify 用），未配置则回退 turnstile.secret_key */
    public String secretKey() {
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
     * 校验 Turnstile token；未启用时直接放行（开发便利）。
     */
    public void verify(String token) {
        if (!isEnabled()) {
            return;
        }
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "人机校验未通过，请刷新后重试");
        }
        String secret = secretKey();
        Map<String, Object> body = new HashMap<>();
        body.put("secret", secret == null ? "" : secret);
        body.put("response", token);
        try {
            String resp = HttpUtil.post(SITEVERIFY_URL, body, HTTP_TIMEOUT_MS);
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

    /**
     * 前端渲染所需的公开配置（只暴露 siteKey，不暴露 secret）
     */
    public Map<String, Object> publicConfig() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("enabled", isEnabled());
        cfg.put("mode", mode());
        cfg.put("siteKey", siteKey());
        cfg.put("emailCodeEnabled", configService.getBool("email_code.enabled", false));
        return cfg;
    }
}
