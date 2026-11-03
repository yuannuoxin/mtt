package com.mtd.tt.controller;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.model.req.OAuthCallbackReq;
import com.mtd.tt.model.req.OAuthOidcReq;
import com.mtd.tt.model.req.OAuthUnbindReq;
import com.mtd.tt.model.resp.OAuthBindingsResp;
import com.mtd.tt.model.resp.OAuthCallbackResp;
import com.mtd.tt.model.resp.OAuthProviderItem;
import com.mtd.tt.service.OAuthService;
import com.mtd.tt.util.LoginUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第三方登录（Google OAuth2.0）：前端配置 / 授权回调 / 绑定总览 / 解绑。
 */
@Tag(name = "第三方登录", description = "Google OAuth 登录、绑定、解绑")
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oauthService;

    @Operation(summary = "Google 登录前端配置（是否启用 + clientId + redirectUri）")
    @PostMapping("/google/config")
    public Result<OAuthProviderItem> googleConfig() {
        return Result.success(oauthService.googleConfig());
    }

    @Operation(summary = "Google 授权回调：未登录=登录/自动注册，已登录=绑定到当前用户")
    @PostMapping("/google/callback")
    public Result<OAuthCallbackResp> googleCallback(@RequestBody OAuthCallbackReq req) {
        return Result.success(oauthService.googleCallback(req.getCode(), req.getRedirectUri(), req.getNonce(), req.getFingerprint()));
    }

    @Operation(summary = "Google OIDC 登录（隐式流，后端本地验签 id_token，不跟 Google 交互）")
    @PostMapping("/google/oidcLogin")
    public Result<OAuthCallbackResp> googleOidcLogin(@RequestBody OAuthOidcReq req) {
        return Result.success(oauthService.googleOidcLogin(req.getIdToken(), req.getNonce(), req.getFingerprint()));
    }

    @Operation(summary = "GitHub 登录前端配置（是否启用 + clientId + redirectUri）")
    @PostMapping("/github/config")
    public Result<OAuthProviderItem> githubConfig() {
        return Result.success(oauthService.githubConfig());
    }

    @Operation(summary = "GitHub 授权回调：未登录=登录/自动注册，已登录=绑定到当前用户")
    @PostMapping("/github/callback")
    public Result<OAuthCallbackResp> githubCallback(@RequestBody OAuthCallbackReq req) {
        return Result.success(oauthService.githubCallback(req.getCode(), req.getRedirectUri(), req.getFingerprint()));
    }

    @Operation(summary = "Telegram OIDC 前端配置（是否启用 + clientId + redirectUri）")
    @PostMapping("/telegram/config")
    public Result<OAuthProviderItem> telegramConfig() {
        return Result.success(oauthService.telegramConfig());
    }

    @Operation(summary = "Telegram OIDC 授权回调：未登录=登录/自动注册，已登录=绑定到当前用户")
    @PostMapping("/telegram/callback")
    public Result<OAuthCallbackResp> telegramCallback(@RequestBody OAuthCallbackReq req) {
        return Result.success(oauthService.telegramCallback(req.getCode(), req.getRedirectUri(), req.getCodeVerifier(), req.getFingerprint()));
    }

    @Operation(summary = "Discord 登录前端配置（是否启用 + clientId + redirectUri）")
    @PostMapping("/discord/config")
    public Result<OAuthProviderItem> discordConfig() {
        return Result.success(oauthService.discordConfig());
    }

    @Operation(summary = "Discord 授权回调：未登录=登录/自动注册，已登录=绑定到当前用户")
    @PostMapping("/discord/callback")
    public Result<OAuthCallbackResp> discordCallback(@RequestBody OAuthCallbackReq req) {
        return Result.success(oauthService.discordCallback(req.getCode(), req.getRedirectUri(), req.getFingerprint()));
    }

    @Operation(summary = "当前用户三方绑定总览（需登录）")
    @PostMapping("/bindings")
    public Result<OAuthBindingsResp> bindings() {
        Long userId = requireLogin();
        return Result.success(oauthService.bindings(userId));
    }

    @Operation(summary = "解绑三方账号（需登录）")
    @PostMapping("/unbind")
    public Result<Void> unbind(@RequestBody OAuthUnbindReq req) {
        Long userId = requireLogin();
        oauthService.unbind(req.getProvider(), userId);
        return Result.success();
    }

    private Long requireLogin() {
        Long userId = LoginUtil.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        return userId;
    }
}
