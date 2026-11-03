package com.mtd.tt.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.enums.OauthProviderEnum;
import com.mtd.tt.enums.UserStatusEnum;
import com.mtd.tt.mapper.OauthBindingMapper;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.model.dto.GoogleUserInfo;
import com.mtd.tt.model.dto.OAuthProviderUser;
import com.mtd.tt.model.entity.OauthBindingEntity;
import com.mtd.tt.model.entity.UserEntity;
import com.mtd.tt.model.resp.LoginResp;
import com.mtd.tt.model.resp.OAuthBindingItem;
import com.mtd.tt.model.resp.OAuthBindingsResp;
import com.mtd.tt.model.resp.OAuthCallbackResp;
import com.mtd.tt.model.resp.OAuthProviderItem;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mtd.tt.util.LoginUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.core.util.HexUtil;

/**
 * 第三方登录（Google OAuth2.0 授权码流程）。
 * <p>
 * 流程：前端带 client_id/redirect_uri/state 跳 Google 授权页 → Google 回调前端路由
 * /oauth/google/callback 带回 code → 前端把 code POST 到 /oauth/google/callback →
 * 后端用 client_secret 换 access_token 并拉取 userinfo → 未登录则登录/自动注册，
 * 已登录则绑定到当前用户。绑定/解绑由 /oauth/bindings、/oauth/unbind 管理。
 * <p>
 * 配置全部走 sys_config（oauth.google.enabled / client_id / client_secret / redirect_uri），不硬编码。
 */
@Slf4j
@Service
public class OAuthService {

    private static final String CFG_GOOGLE_ENABLED = "oauth.google.enabled";
    private static final String CFG_GOOGLE_CLIENT_ID = "oauth.google.client_id";
    private static final String CFG_GOOGLE_CLIENT_SECRET = "oauth.google.client_secret";
    private static final String CFG_GOOGLE_REDIRECT_URI = "oauth.google.redirect_uri";
    private static final String CFG_GOOGLE_MODE = "oauth.google.mode";
    private static final String CFG_GITHUB_ENABLED = "oauth.github.enabled";
    private static final String CFG_GITHUB_CLIENT_ID = "oauth.github.client_id";
    private static final String CFG_GITHUB_CLIENT_SECRET = "oauth.github.client_secret";
    private static final String CFG_GITHUB_REDIRECT_URI = "oauth.github.redirect_uri";
    private static final String CFG_TELEGRAM_ENABLED = "oauth.telegram.enabled";
    private static final String CFG_TELEGRAM_CLIENT_ID = "oauth.telegram.client_id";
    private static final String CFG_TELEGRAM_CLIENT_SECRET = "oauth.telegram.client_secret";
    private static final String CFG_TELEGRAM_REDIRECT_URI = "oauth.telegram.redirect_uri";
    private static final String CFG_DISCORD_ENABLED = "oauth.discord.enabled";
    private static final String CFG_DISCORD_CLIENT_ID = "oauth.discord.client_id";
    private static final String CFG_DISCORD_CLIENT_SECRET = "oauth.discord.client_secret";
    private static final String CFG_DISCORD_REDIRECT_URI = "oauth.discord.redirect_uri";
    private static final String CFG_PROXY_ENABLED = "oauth.proxy.enabled";
    private static final String CFG_PROXY_HOST = "oauth.proxy.host";
    private static final String CFG_PROXY_PORT = "oauth.proxy.port";

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";
    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";
    private static final String TELEGRAM_AUTHORIZE_URL = "https://oauth.telegram.org/auth";
    private static final String TELEGRAM_TOKEN_URL = "https://oauth.telegram.org/token";
    private static final String TELEGRAM_JWKS_URL = "https://oauth.telegram.org/.well-known/jwks.json";
    private static final String DISCORD_AUTHORIZE_URL = "https://discord.com/oauth2/authorize";
    private static final String DISCORD_TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String DISCORD_USER_URL = "https://discord.com/api/users/@me";
    private static final String DISCORD_AVATAR_CDN = "https://cdn.discordapp.com/avatars/";

    @Autowired
    private SysConfigService configService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OauthBindingMapper bindingMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private PointService pointService;
    @Autowired
    private LoginLogService loginLogService;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS oauth_bindings
                    (
                        id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        user_id          BIGINT       NOT NULL,
                        provider         VARCHAR(20)  NOT NULL,
                        provider_user_id VARCHAR(128) NOT NULL,
                        email            VARCHAR(120),
                        display_name     VARCHAR(100),
                        avatar           VARCHAR(255),
                        access_token     VARCHAR(512),
                        refresh_token    VARCHAR(512),
                        created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                        UNIQUE (provider, provider_user_id),
                        UNIQUE (user_id, provider)
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_oauth_bindings_user ON oauth_bindings (user_id)");
            // PostgreSQL 不支持 ADD CONSTRAINT IF NOT EXISTS，需先查 pg_constraint 再决定是否添加
            Long constraintCnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uk_oauth_bindings_user_provider'", Long.class);
            if (constraintCnt == null || constraintCnt == 0) {
                jdbcTemplate.execute("ALTER TABLE oauth_bindings ADD CONSTRAINT uk_oauth_bindings_user_provider UNIQUE (user_id, provider)");
            }
            log.info("第三方绑定表初始化完成");
        } catch (Exception e) {
            log.error("第三方绑定表初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 三方登录前端配置：返回 Google 是否启用、clientId 与 redirectUri（不泄露 secret）
     */
    public OAuthProviderItem googleConfig() {
        OAuthProviderItem item = new OAuthProviderItem();
        item.setProvider(OauthProviderEnum.GOOGLE.getCode());
        item.setName(OauthProviderEnum.GOOGLE.getName());
        item.setEnabled(configService.getBool(CFG_GOOGLE_ENABLED, false));
        item.setClientId(configService.get(CFG_GOOGLE_CLIENT_ID));
        item.setRedirectUri(configService.get(CFG_GOOGLE_REDIRECT_URI));
        item.setMode(StrUtil.blankToDefault(configService.get(CFG_GOOGLE_MODE), "code"));
        Long userId = LoginUtil.currentUserId();
        if (userId != null) {
            long cnt = bindingMapper.selectCountByQuery(
                    QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                            .eq(OauthBindingEntity::getProvider, OauthProviderEnum.GOOGLE.getCode()));
            item.setBound(cnt > 0);
        }
        return item;
    }

    /**
     * Google 授权回调：未登录=登录/自动注册，已登录=绑定到当前用户。
     */
    public OAuthCallbackResp googleCallback(String code, String redirectUri, String nonce, String fingerprint) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "授权码不能为空");
        }
        ensureGoogleEnabled();
        GoogleUserInfo info = exchangeGoogle(code, redirectUri, nonce);
        if (StrUtil.isBlank(info.id())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 Google 账号信息失败");
        }
        OAuthCallbackResp resp = new OAuthCallbackResp();
        Long loginUserId = LoginUtil.currentUserId();
        if (loginUserId == null) {
            resp.setAction("login");
            resp.setLogin(loginByProvider(OauthProviderEnum.GOOGLE, toProviderUser(info), fingerprint));
        } else {
            bindProvider(OauthProviderEnum.GOOGLE, loginUserId, toProviderUser(info));
            resp.setAction("bound");
            resp.setBindings(listBindings(loginUserId));
        }
        return resp;
    }

    /**
     * Google OIDC 登录（隐式流，后端不跟 Google 交互）：
     * 前端授权回调直接拿到 id_token（JWT），后端用 Google JWKS 公钥本地验签，
     * 校验 iss/aud/exp/sub 后复用登录/绑定逻辑。适用于无法直连 Google API（换 token）的环境。
     */
    public OAuthCallbackResp googleOidcLogin(String idToken, String nonce, String fingerprint) {
        if (StrUtil.isBlank(idToken)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Google 登录凭证不能为空");
        }
        ensureGoogleEnabled();
        OAuthProviderUser info = toProviderUser(verifyGoogleIdToken(idToken, nonce));
        OAuthCallbackResp resp = new OAuthCallbackResp();
        Long loginUserId = LoginUtil.currentUserId();
        if (loginUserId == null) {
            resp.setAction("login");
            resp.setLogin(loginByProvider(OauthProviderEnum.GOOGLE, info, fingerprint));
        } else {
            bindProvider(OauthProviderEnum.GOOGLE, loginUserId, info);
            resp.setAction("bound");
            resp.setBindings(listBindings(loginUserId));
        }
        return resp;
    }

    /**
     * GitHub 登录前端配置：是否启用 + clientId + redirectUri（不泄露 secret）
     */
    public OAuthProviderItem githubConfig() {
        OAuthProviderItem item = new OAuthProviderItem();
        item.setProvider(OauthProviderEnum.GITHUB.getCode());
        item.setName(OauthProviderEnum.GITHUB.getName());
        item.setEnabled(configService.getBool(CFG_GITHUB_ENABLED, false));
        item.setClientId(configService.get(CFG_GITHUB_CLIENT_ID));
        item.setRedirectUri(configService.get(CFG_GITHUB_REDIRECT_URI));
        item.setMode("code");
        Long userId = LoginUtil.currentUserId();
        if (userId != null) {
            long cnt = bindingMapper.selectCountByQuery(
                    QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                            .eq(OauthBindingEntity::getProvider, OauthProviderEnum.GITHUB.getCode()));
            item.setBound(cnt > 0);
        }
        return item;
    }

    /**
     * GitHub 授权回调：未登录=登录/自动注册，已登录=绑定到当前用户。
     * GitHub 仅支持授权码模式（无用户 id_token），始终走 code 换 token。
     */
    public OAuthCallbackResp githubCallback(String code, String redirectUri, String fingerprint) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "授权码不能为空");
        }
        if (!configService.getBool(CFG_GITHUB_ENABLED, false)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "GitHub 登录未启用");
        }
        OAuthProviderUser info = exchangeGithub(code, redirectUri);
        if (StrUtil.isBlank(info.id())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 GitHub 账号信息失败");
        }
        OAuthCallbackResp resp = new OAuthCallbackResp();
        Long loginUserId = LoginUtil.currentUserId();
        if (loginUserId == null) {
            resp.setAction("login");
            resp.setLogin(loginByProvider(OauthProviderEnum.GITHUB, info, fingerprint));
        } else {
            bindProvider(OauthProviderEnum.GITHUB, loginUserId, info);
            resp.setAction("bound");
            resp.setBindings(listBindings(loginUserId));
        }
        return resp;
    }

    /**
     * Telegram OIDC 前端配置：clientId + redirectUri（不泄露 secret），授权码 + PKCE 流程。
     */
    public OAuthProviderItem telegramConfig() {
        OAuthProviderItem item = new OAuthProviderItem();
        item.setProvider(OauthProviderEnum.TELEGRAM.getCode());
        item.setName(OauthProviderEnum.TELEGRAM.getName());
        item.setEnabled(configService.getBool(CFG_TELEGRAM_ENABLED, false));
        item.setClientId(configService.get(CFG_TELEGRAM_CLIENT_ID));
        item.setRedirectUri(configService.get(CFG_TELEGRAM_REDIRECT_URI));
        item.setMode("code");
        Long userId = LoginUtil.currentUserId();
        if (userId != null) {
            long cnt = bindingMapper.selectCountByQuery(
                    QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                            .eq(OauthBindingEntity::getProvider, OauthProviderEnum.TELEGRAM.getCode()));
            item.setBound(cnt > 0);
        }
        return item;
    }

    /**
     * Telegram OIDC 授权回调：未登录=登录/自动注册，已登录=绑定到当前用户。
     * 用 client_id:client_secret Basic 认证换 id_token，本地 JWKS 验签（iss/aud/exp）。
     * Telegram 不返回邮箱，注册时用 {id}@telegram.local 占位邮箱。
     */
    public OAuthCallbackResp telegramCallback(String code, String redirectUri, String codeVerifier, String fingerprint) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "授权码不能为空");
        }
        if (!configService.getBool(CFG_TELEGRAM_ENABLED, false)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Telegram 登录未启用");
        }
        OAuthProviderUser info = exchangeTelegram(code, redirectUri, codeVerifier);
        if (StrUtil.isBlank(info.id())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 Telegram 账号信息失败");
        }
        OAuthCallbackResp resp = new OAuthCallbackResp();
        Long loginUserId = LoginUtil.currentUserId();
        if (loginUserId == null) {
            resp.setAction("login");
            resp.setLogin(loginByProvider(OauthProviderEnum.TELEGRAM, info, fingerprint));
        } else {
            bindProvider(OauthProviderEnum.TELEGRAM, loginUserId, info);
            resp.setAction("bound");
            resp.setBindings(listBindings(loginUserId));
        }
        return resp;
    }

    /**
     * Discord 登录前端配置：clientId + redirectUri（不泄露 secret）。
     * Discord 仅支持授权码模式（纯 OAuth2，不提供 OpenID Connect / id_token）。
     */
    public OAuthProviderItem discordConfig() {
        OAuthProviderItem item = new OAuthProviderItem();
        item.setProvider(OauthProviderEnum.DISCORD.getCode());
        item.setName(OauthProviderEnum.DISCORD.getName());
        item.setEnabled(configService.getBool(CFG_DISCORD_ENABLED, false));
        item.setClientId(configService.get(CFG_DISCORD_CLIENT_ID));
        item.setRedirectUri(configService.get(CFG_DISCORD_REDIRECT_URI));
        item.setMode("code");
        Long userId = LoginUtil.currentUserId();
        if (userId != null) {
            long cnt = bindingMapper.selectCountByQuery(
                    QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                            .eq(OauthBindingEntity::getProvider, OauthProviderEnum.DISCORD.getCode()));
            item.setBound(cnt > 0);
        }
        return item;
    }

    /**
     * Discord 授权回调：未登录=登录/自动注册，已登录=绑定到当前用户。
     * 仅授权码模式：用 client_secret 换 access_token，从 token 响应的 user 字段（或 /users/@me）取账号信息。
     */
    public OAuthCallbackResp discordCallback(String code, String redirectUri, String fingerprint) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "授权码不能为空");
        }
        if (!configService.getBool(CFG_DISCORD_ENABLED, false)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Discord 登录未启用");
        }
        OAuthProviderUser info = exchangeDiscord(code, redirectUri);
        if (StrUtil.isBlank(info.id())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 Discord 账号信息失败");
        }
        OAuthCallbackResp resp = new OAuthCallbackResp();
        Long loginUserId = LoginUtil.currentUserId();
        if (loginUserId == null) {
            resp.setAction("login");
            resp.setLogin(loginByProvider(OauthProviderEnum.DISCORD, info, fingerprint));
        } else {
            bindProvider(OauthProviderEnum.DISCORD, loginUserId, info);
            resp.setAction("bound");
            resp.setBindings(listBindings(loginUserId));
        }
        return resp;
    }

    /**
     * Discord 授权码换 access_token 并取用户信息。
     * Discord 不返回 id_token；token 响应在授权 scope 含 identify/email 时直接携带 user 字段，
     * 缺失则回退调 /users/@me。头像为 CDN 地址（avatar hash 非空时）。
     */
    private OAuthProviderUser exchangeDiscord(String code, String redirectUri) {
        String clientId = configService.get(CFG_DISCORD_CLIENT_ID);
        String clientSecret = configService.get(CFG_DISCORD_CLIENT_SECRET);
        if (StrUtil.isBlank(clientId) || StrUtil.isBlank(clientSecret)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Discord 登录未配置");
        }
        if (StrUtil.isBlank(redirectUri)) {
            redirectUri = configService.get(CFG_DISCORD_REDIRECT_URI);
        }
        String tokenBody;
        try {
            tokenBody = applyProxy(HttpRequest.post(DISCORD_TOKEN_URL))
                    .header("Accept", "application/json")
                    .form("grant_type", "authorization_code")
                    .form("code", code)
                    .form("redirect_uri", StrUtil.blankToDefault(redirectUri, ""))
                    .form("client_id", clientId)
                    .form("client_secret", clientSecret)
                    .timeout(15000)
                    .execute().body();
        } catch (Exception e) {
            log.warn("Discord 换 token 失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Discord 授权失败，请重试");
        }
        JSONObject tokenJson = JSONUtil.parseObj(tokenBody);
        String accessToken = tokenJson.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            log.warn("Discord token 响应异常: {}", tokenBody);
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Discord 授权失败，请重试");
        }
        JSONObject u = tokenJson.getJSONObject("user");
        if (u == null) {
            try {
                String userBody = applyProxy(HttpRequest.get(DISCORD_USER_URL))
                        .header("Authorization", "Bearer " + accessToken)
                        .timeout(15000)
                        .execute().body();
                u = JSONUtil.parseObj(userBody);
            } catch (Exception e) {
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 Discord 账号信息失败");
            }
        }
        if (u == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 Discord 账号信息失败");
        }
        String id = String.valueOf(u.get("id"));
        String email = u.getStr("email"); // Discord 邮箱可选且可能未验证
        String avatarHash = u.getStr("avatar");
        String avatar = StrUtil.isBlank(avatarHash) ? null
                : DISCORD_AVATAR_CDN + id + "/" + avatarHash + ".png";
        String name = StrUtil.blankToDefault(u.getStr("global_name"),
                StrUtil.blankToDefault(u.getStr("username"), id));
        return new OAuthProviderUser(id, email, name, avatar);
    }

    /** Telegram OIDC：用授权码换 id_token 并本地验签，解析用户 claims。 */
    private OAuthProviderUser exchangeTelegram(String code, String redirectUri, String codeVerifier) {
        String clientId = configService.get(CFG_TELEGRAM_CLIENT_ID);
        String clientSecret = configService.get(CFG_TELEGRAM_CLIENT_SECRET);
        if (StrUtil.isBlank(clientId) || StrUtil.isBlank(clientSecret)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Telegram 登录未配置");
        }
        if (StrUtil.isBlank(redirectUri)) {
            redirectUri = configService.get(CFG_TELEGRAM_REDIRECT_URI);
        }
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String tokenBody;
        try {
            tokenBody = applyProxy(HttpRequest.post(TELEGRAM_TOKEN_URL))
                    .header("Authorization", "Basic " + basic)
                    .header("Accept", "application/json")
                    .form("grant_type", "authorization_code")
                    .form("code", code)
                    .form("redirect_uri", StrUtil.blankToDefault(redirectUri, ""))
                    .form("client_id", clientId)
                    .form("code_verifier", StrUtil.blankToDefault(codeVerifier, ""))
                    .timeout(15000)
                    .execute().body();
        } catch (Exception e) {
            log.warn("Telegram 换 token 失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Telegram 授权失败，请重试");
        }
        JSONObject tokenJson = JSONUtil.parseObj(tokenBody);
        String idToken = tokenJson.getStr("id_token");
        if (StrUtil.isBlank(idToken)) {
            log.warn("Telegram token 响应异常: {}", tokenBody);
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Telegram 授权失败，请重试");
        }
        JSONObject claims = verifyTelegramIdToken(idToken);
        String id = StrUtil.blankToDefault(claims.getStr("sub"), String.valueOf(claims.get("id")));
        String name = StrUtil.blankToDefault(claims.getStr("name"),
                StrUtil.blankToDefault(claims.getStr("preferred_username"), id));
        return new OAuthProviderUser(id, id + "@" + OauthProviderEnum.TELEGRAM.getCode() + ".local",
                name, claims.getStr("picture"));
    }

    /**
     * 本地校验 Telegram id_token：iss=oauth.telegram.org、aud=client_id、exp 未过期，JWKS 公钥 RS256 验签。
     */
    private JSONObject verifyTelegramIdToken(String idToken) {
        String clientId = configService.get(CFG_TELEGRAM_CLIENT_ID);
        JWT jwt;
        try {
            jwt = JWTUtil.parseToken(idToken);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Telegram 登录凭证无效");
        }
        JSONObject payload = jwt.getPayloads();
        String iss = payload.getStr("iss");
        String aud = payload.getStr("aud");
        Long exp = payload.getLong("exp");
        if (!"https://oauth.telegram.org".equals(iss)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Telegram 登录凭证无效");
        }
        if (StrUtil.isNotBlank(clientId) && !clientId.equals(aud)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Telegram 登录凭证无效");
        }
        if (exp == null || exp * 1000 <= System.currentTimeMillis() - 60_000) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Telegram 登录凭证已过期");
        }
        Object kidObj = jwt.getHeader("kid");
        RSAPublicKey pub = kidObj == null ? null : telegramJwksPublicKey(String.valueOf(kidObj));
        if (pub == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Telegram 登录凭证校验失败");
        }
        try {
            JWTUtil.verify(idToken, JWTSignerUtil.rs256(pub));
        } catch (Exception e) {
            log.warn("Telegram id_token 签名校验失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Telegram 登录凭证校验失败");
        }
        return payload;
    }

    // ---- Telegram JWKS（OIDC 公钥）----
    private volatile Map<String, RSAPublicKey> telegramJwksKeys = new HashMap<>();
    private volatile long telegramJwksFetchedAt = 0;

    private RSAPublicKey telegramJwksPublicKey(String kid) {
        if (StrUtil.isBlank(kid)) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (telegramJwksKeys.containsKey(kid) && now - telegramJwksFetchedAt < JWKS_TTL_MS) {
            return telegramJwksKeys.get(kid);
        }
        synchronized (this) {
            if (now - telegramJwksFetchedAt > JWKS_TTL_MS || !telegramJwksKeys.containsKey(kid)) {
                fetchTelegramJwks();
            }
            return telegramJwksKeys.get(kid);
        }
    }

    private void fetchTelegramJwks() {
        try {
            String body = applyProxy(HttpRequest.get(TELEGRAM_JWKS_URL)).timeout(15000).execute().body();
            JSONObject json = JSONUtil.parseObj(body);
            JSONArray keys = json.getJSONArray("keys");
            Map<String, RSAPublicKey> m = new HashMap<>();
            if (keys != null) {
                for (int i = 0; i < keys.size(); i++) {
                    JSONObject k = keys.getJSONObject(i);
                    String kid = k.getStr("kid");
                    String n = k.getStr("n");
                    String e = k.getStr("e");
                    if (kid == null || n == null || e == null) {
                        continue;
                    }
                    try {
                        byte[] nb = Base64.getUrlDecoder().decode(n);
                        byte[] eb = Base64.getUrlDecoder().decode(e);
                        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nb), new BigInteger(1, eb));
                        m.put(kid, (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec));
                    } catch (Exception ex) {
                        log.warn("解析 Telegram JWK 失败: {}", ex.getMessage());
                    }
                }
            }
            telegramJwksKeys = m;
            telegramJwksFetchedAt = System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("拉取 Telegram JWKS 失败: {}", e.getMessage());
        }
    }

    /**
     * 当前用户三方绑定总览：系统支持的三方列表 + 已绑定明细
     */
    public OAuthBindingsResp bindings(Long userId) {
        List<OauthBindingEntity> binds = bindingMapper.selectListByQuery(
                QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                        .orderBy(OauthBindingEntity::getId, true));
        OAuthBindingsResp resp = new OAuthBindingsResp();
        resp.setBindings(binds.stream().map(this::toItem).toList());
        List<OAuthProviderItem> providers = new ArrayList<>();
        for (OauthProviderEnum p : OauthProviderEnum.values()) {
            OAuthProviderItem item = new OAuthProviderItem();
            item.setProvider(p.getCode());
            item.setName(p.getName());
            if (p == OauthProviderEnum.GOOGLE) {
                item.setEnabled(configService.getBool(CFG_GOOGLE_ENABLED, false));
                item.setClientId(configService.get(CFG_GOOGLE_CLIENT_ID));
                item.setRedirectUri(configService.get(CFG_GOOGLE_REDIRECT_URI));
                item.setMode(StrUtil.blankToDefault(configService.get(CFG_GOOGLE_MODE), "code"));
            } else if (p == OauthProviderEnum.GITHUB) {
                item.setEnabled(configService.getBool(CFG_GITHUB_ENABLED, false));
                item.setClientId(configService.get(CFG_GITHUB_CLIENT_ID));
                item.setRedirectUri(configService.get(CFG_GITHUB_REDIRECT_URI));
                item.setMode("code");
            } else if (p == OauthProviderEnum.TELEGRAM) {
                item.setEnabled(configService.getBool(CFG_TELEGRAM_ENABLED, false));
                item.setClientId(configService.get(CFG_TELEGRAM_CLIENT_ID));
                item.setRedirectUri(configService.get(CFG_TELEGRAM_REDIRECT_URI));
                item.setMode("code");
            } else if (p == OauthProviderEnum.DISCORD) {
                item.setEnabled(configService.getBool(CFG_DISCORD_ENABLED, false));
                item.setClientId(configService.get(CFG_DISCORD_CLIENT_ID));
                item.setRedirectUri(configService.get(CFG_DISCORD_REDIRECT_URI));
                item.setMode("code");
            }
            item.setBound(binds.stream().anyMatch(b -> p.getCode().equals(b.getProvider())));
            providers.add(item);
        }
        resp.setProviders(providers);
        return resp;
    }

    /**
     * 解绑三方：要求用户至少保留一种登录方式（有密码、或仍剩其它三方绑定）。
     */
    public void unbind(String provider, Long userId) {
        if (StrUtil.isBlank(provider)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "provider 不能为空");
        }
        OauthBindingEntity bind = bindingMapper.selectOneByQuery(
                QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                        .eq(OauthBindingEntity::getProvider, provider));
        if (bind == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该三方账号未绑定");
        }
        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserEntity::getId, userId));
        long otherBinds = bindingMapper.selectCountByQuery(
                QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                        .ne(OauthBindingEntity::getId, bind.getId()));
        boolean hasPassword = user != null && StrUtil.isNotBlank(user.getPasswordHash());
        if (!hasPassword && otherBinds <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "至少保留一种登录方式，请先设置密码后再解绑");
        }
        bindingMapper.deleteById(bind.getId());
    }

    /**
     * 登录/自动注册（未登录模式，provider 无关）
     */
    private LoginResp loginByProvider(OauthProviderEnum provider, OAuthProviderUser info, String fingerprint) {
        String providerCode = provider.getCode();
        OauthBindingEntity bind = findByProviderUser(providerCode, info.id());
        if (bind != null) {
            UserEntity user = userMapper.selectOneByQuery(
                    QueryWrapper.create().eq(UserEntity::getId, bind.getUserId()));
            checkCanLogin(user);
            return doLogin(user, providerCode, fingerprint);
        }
        // 未绑定：用三方邮箱查本地用户。邮箱一致即证明该三方账号属于该本地账号，自动绑定并直接登录。
        // 仅当该三方账号已被其他账号绑定时拒绝（防接管）。
        UserEntity byEmail = StrUtil.isBlank(info.email()) ? null
                : userMapper.selectOneByQuery(QueryWrapper.create().eq(UserEntity::getEmail, info.email()));
        if (byEmail != null) {
            OauthBindingEntity other = findByProviderUser(providerCode, info.id());
            if (other != null && !other.getUserId().equals(byEmail.getId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                        "该" + provider.getName() + "账号已绑定其他账号");
            }
            // 邮箱一致证明该三方账号属于该本地账号。若该本地账号已绑定同 provider，
            // 直接登录即可，避免重复绑定触发 UNIQUE(user_id, provider) 约束冲突。
            long sameProviderCount = bindingMapper.selectCountByQuery(
                    QueryWrapper.create().eq(OauthBindingEntity::getUserId, byEmail.getId())
                            .eq(OauthBindingEntity::getProvider, providerCode));
            if (sameProviderCount > 0) {
                return doLogin(byEmail, providerCode, fingerprint);
            }
            bind(provider, byEmail.getId(), info, null);
            return doLogin(byEmail, providerCode, fingerprint);
        }
        UserEntity user = registerByOAuth(provider, info, fingerprint);
        bind(provider, user.getId(), info, null);
        return doLogin(user, providerCode, fingerprint);
    }

    /**
     * 绑定（已登录模式）：三方账号已被其他用户绑定则拒绝；同一本地账号对同一 provider 只允许绑定一个三方账号。
     */
    private void bindProvider(OauthProviderEnum provider, Long userId, OAuthProviderUser info) {
        OauthBindingEntity exist = findByProviderUser(provider.getCode(), info.id());
        if (exist != null) {
            if (exist.getUserId().equals(userId)) {
                return; // 已绑定到当前用户，幂等
            }
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该" + provider.getName() + "账号已被其他用户绑定");
        }
        long sameProviderCount = bindingMapper.selectCountByQuery(
                QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                        .eq(OauthBindingEntity::getProvider, provider.getCode()));
        if (sameProviderCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该账号已绑定" + provider.getName() + "账号，请先解绑后再绑定新账号");
        }
        bind(provider, userId, info, null);
    }

    private void bind(OauthProviderEnum provider, Long userId, OAuthProviderUser info, String accessToken) {
        OauthBindingEntity b = new OauthBindingEntity();
        b.setUserId(userId);
        b.setProvider(provider.getCode());
        b.setProviderUserId(info.id());
        b.setEmail(info.email());
        b.setDisplayName(info.name());
        b.setAvatar(info.avatar());
        b.setAccessToken(accessToken);
        b.setCreatedAt(OffsetDateTime.now());
        bindingMapper.insertSelective(b);
    }

    /**
     * 用三方资料自动注册新用户（无密码，可后续设置；注册送积分）。
     * 无邮箱的三方（如 Telegram）用 {id}@{provider}.local 占位邮箱，保证账号体系一致。
     */
    private UserEntity registerByOAuth(OauthProviderEnum provider, OAuthProviderUser info, String fingerprint) {
        String email = info.email();
        if (StrUtil.isBlank(email)) {
            email = info.id() + "@" + provider.getCode() + ".local";
        }
        UserEntity user = new UserEntity();
        user.setUsername(uniqueUsername(email, info.name()));
        user.setNickname(StrUtil.isBlank(info.name()) ? user.getUsername() : info.name());
        user.setEmail(email);
        user.setPasswordHash(null);
        user.setStatus(UserStatusEnum.ACTIVE.getCode());
        user.setAvatar(info.avatar());
        user.setFingerprint(fingerprint);
        user.setPoints((long) pointService.initialPoint());
        userMapper.insertSelective(user);
        pointService.grantRegisterPoints(user.getId());
        return user;
    }

    /**
     * 生成唯一用户名：优先用邮箱前缀，被占用则追加随机后缀。
     */
    private String uniqueUsername(String email, String name) {
        String base = "";
        if (StrUtil.isNotBlank(email)) {
            base = email.substring(0, email.indexOf('@')).replaceAll("[^A-Za-z0-9_]", "");
        }
        if (StrUtil.isBlank(base) && StrUtil.isNotBlank(name)) {
            base = name.replaceAll("[^A-Za-z0-9_]", "");
        }
        if (StrUtil.isBlank(base)) {
            base = "user" + RandomUtil.randomNumbers(6);
        }
        if (base.length() > 50) {
            base = base.substring(0, 50);
        }
        if (isUsernameTaken(base)) {
            return base + RandomUtil.randomNumbers(4);
        }
        return base;
    }

    private boolean isUsernameTaken(String username) {
        return userMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserEntity::getUsername, username)) != null;
    }

    /**
     * 用授权码换 access_token 并拉取 Google 用户信息
     */
    private GoogleUserInfo exchangeGoogle(String code, String redirectUri, String nonce) {
        String clientId = configService.get(CFG_GOOGLE_CLIENT_ID);
        String clientSecret = configService.get(CFG_GOOGLE_CLIENT_SECRET);
        if (StrUtil.isBlank(clientId) || StrUtil.isBlank(clientSecret)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Google 登录未配置");
        }
        if (StrUtil.isBlank(redirectUri)) {
            redirectUri = configService.get(CFG_GOOGLE_REDIRECT_URI);
        }
        String body;
        try {
            body = applyProxy(HttpRequest.post(GOOGLE_TOKEN_URL))
                    .form("grant_type", "authorization_code")
                    .form("code", code)
                    .form("client_id", clientId)
                    .form("client_secret", clientSecret)
                    .form("redirect_uri", StrUtil.blankToDefault(redirectUri, ""))
                    .timeout(15000)
                    .execute().body();
        } catch (Exception e) {
            log.warn("Google 换 token 失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 授权失败，请重试");
        }
        JSONObject json = JSONUtil.parseObj(body);
        String accessToken = json.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            log.warn("Google token 响应异常: {}", body);
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 授权失败，请重试");
        }
        // nonce 防重放：授权时带的 nonce 应等于 Google 换回的 id_token 内 nonce（只有传了 nonce 才校验）
        if (StrUtil.isNotBlank(nonce)) {
            String idToken = json.getStr("id_token");
            if (StrUtil.isBlank(idToken)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 授权失败，请重试");
            }
            try {
                JWT jwt = JWTUtil.parseToken(idToken);
                Object jwtNonce = jwt.getPayload("nonce");
                if (jwtNonce == null || !nonce.equals(String.valueOf(jwtNonce))) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 授权校验失败");
                }
            } catch (BusinessException be) {
                throw be;
            } catch (Exception e) {
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 授权校验失败");
            }
        }
        String ub;
        try {
            ub = applyProxy(HttpRequest.get(GOOGLE_USERINFO_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(15000)
                    .execute().body();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 Google 账号信息失败");
        }
        JSONObject u = JSONUtil.parseObj(ub);
        return new GoogleUserInfo(u.getStr("id"), u.getStr("email"),
                u.getStr("name"), u.getStr("picture"));
    }

    /**
     * 用授权码换 access_token 并拉取 GitHub 用户信息。
     * GitHub 不提供 OIDC 用户 id_token，仅支持授权码模式；邮箱默认非公开，需 scope=user:email 并拉 /user/emails。
     */
    private OAuthProviderUser exchangeGithub(String code, String redirectUri) {
        String clientId = configService.get(CFG_GITHUB_CLIENT_ID);
        String clientSecret = configService.get(CFG_GITHUB_CLIENT_SECRET);
        if (StrUtil.isBlank(clientId) || StrUtil.isBlank(clientSecret)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "GitHub 登录未配置");
        }
        if (StrUtil.isBlank(redirectUri)) {
            redirectUri = configService.get(CFG_GITHUB_REDIRECT_URI);
        }
        String tokenBody;
        try {
            tokenBody = applyProxy(HttpRequest.post(GITHUB_TOKEN_URL))
                    .header("Accept", "application/json")
                    .form("client_id", clientId)
                    .form("client_secret", clientSecret)
                    .form("code", code)
                    .form("redirect_uri", StrUtil.blankToDefault(redirectUri, ""))
                    .timeout(15000)
                    .execute().body();
        } catch (Exception e) {
            log.warn("GitHub 换 token 失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "GitHub 授权失败，请重试");
        }
        JSONObject tokenJson = JSONUtil.parseObj(tokenBody);
        String accessToken = tokenJson.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            log.warn("GitHub token 响应异常: {}", tokenBody);
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "GitHub 授权失败，请重试");
        }
        String userBody;
        try {
            userBody = applyProxy(HttpRequest.get(GITHUB_USER_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .timeout(15000)
                    .execute().body();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 GitHub 账号信息失败");
        }
        JSONObject u = JSONUtil.parseObj(userBody);
        String id = String.valueOf(u.get("id"));
        String email = u.getStr("email"); // 邮箱非公开时可能为空
        if (StrUtil.isBlank(email)) {
            email = primaryGithubEmail(accessToken);
        }
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(),
                    "无法获取 GitHub 邮箱，请将 GitHub 邮箱设为公开或用主邮箱验证");
        }
        return new OAuthProviderUser(id, email, StrUtil.blankToDefault(u.getStr("name"), u.getStr("login")),
                u.getStr("avatar_url"));
    }

    /**
     * 拉取 GitHub 主邮箱（scope=user:email 时 /user/emails 可返回验证过的邮箱）。
     */
    private String primaryGithubEmail(String accessToken) {
        try {
            String body = applyProxy(HttpRequest.get(GITHUB_EMAILS_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .timeout(15000)
                    .execute().body();
            JSONArray arr = JSONUtil.parseArray(body);
            if (arr == null) {
                return null;
            }
            for (int i = 0; i < arr.size(); i++) {
                JSONObject it = arr.getJSONObject(i);
                if (Boolean.TRUE.equals(it.getBool("primary"))) {
                    return it.getStr("email");
                }
            }
            for (int i = 0; i < arr.size(); i++) {
                JSONObject it = arr.getJSONObject(i);
                if (StrUtil.isNotBlank(it.getStr("email"))) {
                    return it.getStr("email");
                }
            }
        } catch (Exception e) {
            log.warn("拉取 GitHub 邮箱失败: {}", e.getMessage());
        }
        return null;
    }

    /** Google userinfo → provider 无关的三方用户视图 */
    private OAuthProviderUser toProviderUser(GoogleUserInfo g) {
        return new OAuthProviderUser(g.id(), g.email(), g.name(), g.picture());
    }

    /**
     * 若配置了代理（oauth.proxy.enabled/host/port），给请求挂代理。
     * 用于本机无法直连 Google API 的场景（如需要通过代理/VPN 访问 Google）。
     */
    private HttpRequest applyProxy(HttpRequest req) {
        String enabled = configService.get(CFG_PROXY_ENABLED);
        String host = configService.get(CFG_PROXY_HOST);
        String portStr = configService.get(CFG_PROXY_PORT);
        if (StrUtil.isBlank(enabled) || !"true".equalsIgnoreCase(enabled.trim())) {
            return req;
        }
        if (StrUtil.isBlank(host) || StrUtil.isBlank(portStr)) {
            return req;
        }
        try {
            int port = Integer.parseInt(portStr.trim());
            if (port <= 0) {
                return req;
            }
            req.setProxy(new java.net.Proxy(java.net.Proxy.Type.HTTP,
                    new java.net.InetSocketAddress(host.trim(), port)));
        } catch (NumberFormatException e) {
            log.warn("oauth.proxy.port 配置非法: {}", portStr);
        }
        return req;
    }

    private void ensureGoogleEnabled() {
        if (!configService.getBool(CFG_GOOGLE_ENABLED, false)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Google 登录未启用");
        }
    }

    // ---- OIDC id_token 本地验签（Google JWKS）----
    private static final long JWKS_TTL_MS = 6 * 60 * 60 * 1000L;
    private volatile Map<String, RSAPublicKey> jwksKeys = new HashMap<>();
    private volatile long jwksFetchedAt = 0;

    /**
     * 本地校验 Google id_token：先看 iss/aud/exp/sub/email_verified/nonce，再用 JWKS 公钥验签。
     */
    private GoogleUserInfo verifyGoogleIdToken(String idToken, String nonce) {
        String clientId = configService.get(CFG_GOOGLE_CLIENT_ID);
        JWT jwt;
        try {
            jwt = JWTUtil.parseToken(idToken);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 登录凭证无效");
        }
        JSONObject payload = jwt.getPayloads();
        String iss = payload.getStr("iss");
        String aud = payload.getStr("aud");
        Long exp = payload.getLong("exp");
        String sub = payload.getStr("sub");
        if (!("https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 登录凭证无效");
        }
        if (StrUtil.isNotBlank(clientId) && !clientId.equals(aud)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 登录凭证无效");
        }
        if (exp == null || exp * 1000 <= System.currentTimeMillis() - 60_000) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 登录凭证已过期");
        }
        if (StrUtil.isBlank(sub)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 登录凭证无效");
        }
        Object emailVerified = payload.get("email_verified");
        if (emailVerified != null && !Boolean.TRUE.equals(emailVerified)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 邮箱未验证");
        }
        // nonce 防重放：前端授权时生成的 nonce 应等于 id_token 内 nonce（只有传了 nonce 才校验）
        String jwtNonce = payload.getStr("nonce");
        if (StrUtil.isNotBlank(nonce) && !nonce.equals(jwtNonce)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 登录凭证校验失败");
        }
        Object kidObj = jwt.getHeader("kid");
        RSAPublicKey pub = kidObj == null ? null : jwksPublicKey(String.valueOf(kidObj));
        if (pub == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 登录凭证校验失败");
        }
        try {
            JWTUtil.verify(idToken, JWTSignerUtil.rs256(pub));
        } catch (Exception e) {
            log.warn("Google id_token 签名校验失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Google 登录凭证校验失败");
        }
        return new GoogleUserInfo(sub, payload.getStr("email"), payload.getStr("name"), payload.getStr("picture"));
    }

    /**
     * 按 kid 取 Google JWKS 公钥，缓存 6 小时；缓存失效或未命中时重新拉取（走代理）。
     */
    private RSAPublicKey jwksPublicKey(String kid) {
        if (StrUtil.isBlank(kid)) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (jwksKeys.containsKey(kid) && now - jwksFetchedAt < JWKS_TTL_MS) {
            return jwksKeys.get(kid);
        }
        synchronized (this) {
            if (now - jwksFetchedAt > JWKS_TTL_MS || !jwksKeys.containsKey(kid)) {
                fetchJwks();
            }
            return jwksKeys.get(kid);
        }
    }

    private void fetchJwks() {
        try {
            String body = applyProxy(HttpRequest.get(GOOGLE_JWKS_URL)).timeout(15000).execute().body();
            JSONObject json = JSONUtil.parseObj(body);
            JSONArray keys = json.getJSONArray("keys");
            Map<String, RSAPublicKey> m = new HashMap<>();
            if (keys != null) {
                for (int i = 0; i < keys.size(); i++) {
                    JSONObject k = keys.getJSONObject(i);
                    String kid = k.getStr("kid");
                    String n = k.getStr("n");
                    String e = k.getStr("e");
                    if (kid == null || n == null || e == null) {
                        continue;
                    }
                    try {
                        byte[] nb = Base64.getUrlDecoder().decode(n);
                        byte[] eb = Base64.getUrlDecoder().decode(e);
                        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nb), new BigInteger(1, eb));
                        m.put(kid, (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec));
                    } catch (Exception ex) {
                        log.warn("解析 Google JWK 失败: {}", ex.getMessage());
                    }
                }
            }
            jwksKeys = m;
            jwksFetchedAt = System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("拉取 Google JWKS 失败: {}", e.getMessage());
        }
    }

    private OauthBindingEntity findByProviderUser(String provider, String providerUserId) {
        if (StrUtil.isBlank(providerUserId)) {
            return null;
        }
        return bindingMapper.selectOneByQuery(
                QueryWrapper.create().eq(OauthBindingEntity::getProvider, provider)
                        .eq(OauthBindingEntity::getProviderUserId, providerUserId));
    }

    private List<OAuthBindingItem> listBindings(Long userId) {
        return bindingMapper.selectListByQuery(
                QueryWrapper.create().eq(OauthBindingEntity::getUserId, userId)
                        .orderBy(OauthBindingEntity::getId, true))
                .stream().map(this::toItem).toList();
    }

    private OAuthBindingItem toItem(OauthBindingEntity b) {
        OAuthBindingItem item = new OAuthBindingItem();
        item.setProvider(b.getProvider());
        OauthProviderEnum p = OauthProviderEnum.valueOf(b.getProvider().toUpperCase());
        item.setProviderName(p == null ? b.getProvider() : p.getName());
        item.setEmail(b.getEmail());
        item.setDisplayName(b.getDisplayName());
        item.setAvatar(b.getAvatar());
        item.setBoundAt(b.getCreatedAt());
        return item;
    }

    private LoginResp doLogin(UserEntity user, String provider, String fingerprint) {
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        LoginResp resp = new LoginResp();
        resp.setTokenName(tokenInfo.getTokenName());
        resp.setTokenValue(tokenInfo.getTokenValue());
        resp.setUser(userService.getProfile(user.getId(), user.getId()));
        loginLogService.record(user.getId(), provider, fingerprint);
        return resp;
    }

    private void checkCanLogin(UserEntity user) {
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号不存在");
        }
        if (!UserStatusEnum.ACTIVE.getCode().equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }
    }
}
