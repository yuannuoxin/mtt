package com.mtd.tt.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.enums.OauthProviderEnum;
import com.mtd.tt.enums.UserStatusEnum;
import com.mtd.tt.mapper.OauthBindingMapper;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.model.dto.GoogleUserInfo;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

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
                        UNIQUE (provider, provider_user_id)
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_oauth_bindings_user ON oauth_bindings (user_id)");
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
    public OAuthCallbackResp googleCallback(String code, String redirectUri) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "授权码不能为空");
        }
        ensureGoogleEnabled();
        GoogleUserInfo info = exchangeGoogle(code, redirectUri);
        if (StrUtil.isBlank(info.id())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "获取 Google 账号信息失败");
        }
        OAuthCallbackResp resp = new OAuthCallbackResp();
        Long loginUserId = LoginUtil.currentUserId();
        if (loginUserId == null) {
            resp.setAction("login");
            resp.setLogin(loginByGoogle(info));
        } else {
            bindGoogle(loginUserId, info);
            resp.setAction("bound");
            resp.setBindings(listBindings(loginUserId));
        }
        return resp;
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
     * 登录/自动注册（未登录模式）
     */
    private LoginResp loginByGoogle(GoogleUserInfo info) {
        OauthBindingEntity bind = findByProviderUser(OauthProviderEnum.GOOGLE.getCode(), info.id());
        if (bind != null) {
            UserEntity user = userMapper.selectOneByQuery(
                    QueryWrapper.create().eq(UserEntity::getId, bind.getUserId()));
            checkCanLogin(user);
            return doLogin(user);
        }
        // 未绑定：用 Google 邮箱查本地用户
        UserEntity byEmail = StrUtil.isBlank(info.email()) ? null
                : userMapper.selectOneByQuery(QueryWrapper.create().eq(UserEntity::getEmail, info.email()));
        if (byEmail != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该 Google 邮箱已注册，请先登录本地账号后再绑定");
        }
        UserEntity user = registerByOAuth(info);
        bind(user.getId(), info, null);
        return doLogin(user);
    }

    /**
     * 绑定（已登录模式）：Google 账号已被其他用户绑定则拒绝。
     */
    private void bindGoogle(Long userId, GoogleUserInfo info) {
        OauthBindingEntity exist = findByProviderUser(OauthProviderEnum.GOOGLE.getCode(), info.id());
        if (exist != null) {
            if (exist.getUserId().equals(userId)) {
                return; // 已绑定到当前用户，幂等
            }
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该 Google 账号已被其他用户绑定");
        }
        bind(userId, info, null);
    }

    private void bind(Long userId, GoogleUserInfo info, String accessToken) {
        OauthBindingEntity b = new OauthBindingEntity();
        b.setUserId(userId);
        b.setProvider(OauthProviderEnum.GOOGLE.getCode());
        b.setProviderUserId(info.id());
        b.setEmail(info.email());
        b.setDisplayName(info.name());
        b.setAvatar(info.picture());
        b.setAccessToken(accessToken);
        b.setCreatedAt(OffsetDateTime.now());
        bindingMapper.insertSelective(b);
    }

    /**
     * 用 Google 资料自动注册新用户（无密码，可后续设置；注册送积分）
     */
    private UserEntity registerByOAuth(GoogleUserInfo info) {
        UserEntity user = new UserEntity();
        user.setUsername(uniqueUsername(info.email(), info.name()));
        user.setNickname(StrUtil.isBlank(info.name()) ? user.getUsername() : info.name());
        user.setEmail(info.email());
        user.setPasswordHash(null);
        user.setStatus(UserStatusEnum.ACTIVE.getCode());
        user.setAvatar(info.picture());
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
    private GoogleUserInfo exchangeGoogle(String code, String redirectUri) {
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
            body = HttpRequest.post(GOOGLE_TOKEN_URL)
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
        String ub;
        try {
            ub = HttpRequest.get(GOOGLE_USERINFO_URL)
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

    private void ensureGoogleEnabled() {
        if (!configService.getBool(CFG_GOOGLE_ENABLED, false)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Google 登录未启用");
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

    private LoginResp doLogin(UserEntity user) {
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        LoginResp resp = new LoginResp();
        resp.setTokenName(tokenInfo.getTokenName());
        resp.setTokenValue(tokenInfo.getTokenValue());
        resp.setUser(userService.getProfile(user.getId(), user.getId()));
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
