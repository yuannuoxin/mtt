package com.mtd.tt.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.model.entity.UserEntity;
import com.mtd.tt.model.req.EmailCodeLoginReq;
import com.mtd.tt.model.req.ForgotPasswordReq;
import com.mtd.tt.model.req.PasswordLoginReq;
import com.mtd.tt.model.req.RegisterReq;
import com.mtd.tt.model.req.SendEmailCodeReq;
import com.mtd.tt.model.resp.LoginResp;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mtd.tt.enums.UserStatusEnum;
import com.mtd.tt.util.FieldValidator;
import com.mtd.tt.util.LoginUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证服务：注册 / 登录 / 登出 / 验证码
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TurnstileService turnstileService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PointService pointService;

    @Autowired
    private SysConfigService sysConfigService;

    private static final String EMAIL_CODE_PREFIX = "tt:login:email:code:";
    private static final String EMAIL_CODE_FREQ_PREFIX = "tt:login:email:freq:";
    private static final long CODE_EXPIRE_MINUTES = 5;
    private static final long FREQ_LIMIT_SECONDS = 60;

    /** 邮箱域名注册黑名单配置键（sys_config，逗号分隔） */
    private static final String CFG_EMAIL_DOMAIN_BLACKLIST = "email.domain.blacklist";

    /**
     * 密码登录（用户名或邮箱 + 密码）
     */
    public LoginResp passwordLogin(PasswordLoginReq req) {
        turnstileService.verify(req.getTurnstileToken());
        if (StrUtil.isBlank(req.getAccount()) || StrUtil.isBlank(req.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "账号和密码不能为空");
        }
        UserEntity user = findByAccount(req.getAccount());
        checkCanLogin(user);
        if (!BCrypt.checkpw(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "账号或密码错误");
        }
        return doLogin(user);
    }

    /**
     * 注册
     */
    public LoginResp register(RegisterReq req) {
        turnstileService.verify(req.getTurnstileToken());
        if (StrUtil.isBlank(req.getUsername()) || StrUtil.isBlank(req.getNickname())
                || StrUtil.isBlank(req.getEmail()) || StrUtil.isBlank(req.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名、昵称、邮箱、密码均不能为空");
        }
        // 字段格式/长度校验（与编辑、忘记密码共用一套规则）
        FieldValidator.validateUsername(req.getUsername());
        FieldValidator.validateNickname(req.getNickname());
        FieldValidator.validateEmail(req.getEmail());
        FieldValidator.validatePassword(req.getPassword());
        // 邮箱域名黑名单校验（可配置，逗号分隔；命中则禁止注册）
        validateEmailDomain(req.getEmail());
        // 邮箱验证码注册：启用时校验验证码；开发期关闭则跳过（便利）
        if (emailService.isEnabled()) {
            if (StrUtil.isBlank(req.getEmailCode())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请输入邮箱验证码");
            }
            String cacheKey = EMAIL_CODE_PREFIX + req.getEmail();
            String cachedCode = redisTemplate.opsForValue().get(cacheKey);
            if (cachedCode == null || !cachedCode.equals(req.getEmailCode())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "邮箱验证码错误或已过期");
            }
            redisTemplate.delete(cacheKey);
        }
        // 用户名唯一校验
        UserEntity byUsername = userMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserEntity::getUsername, req.getUsername()));
        if (byUsername != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名已被占用");
        }
        // 邮箱唯一校验
        UserEntity byEmail = userMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserEntity::getEmail, req.getEmail()));
        if (byEmail != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "邮箱已被注册");
        }

        UserEntity user = new UserEntity();
        user.setUsername(req.getUsername());
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setPasswordHash(BCrypt.hashpw(req.getPassword(), BCrypt.gensalt()));
        user.setStatus(UserStatusEnum.ACTIVE.getCode());
        // 注册赠送初始积分
        user.setPoints((long) pointService.initialPoint());
        // 浏览器指纹（可选，FingerprintJS v5 MIT 前端生成）
        user.setFingerprint(req.getFingerprint());
        userMapper.insertSelective(user);
        pointService.grantRegisterPoints(user.getId());

        return doLogin(user);
    }

    /**
     * 邮箱域名黑名单校验：sys_config 的 email.domain.blacklist（逗号分隔）命中则拒绝。
     * 支持精确域名与子域名后缀匹配（如黑名单含 example.com，则 a.example.com 也命中）。
     */
    private void validateEmailDomain(String email) {
        if (StrUtil.isBlank(email)) {
            return;
        }
        int at = email.indexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return;
        }
        String domain = email.substring(at + 1).trim().toLowerCase();
        String blacklist = sysConfigService.get(CFG_EMAIL_DOMAIN_BLACKLIST);
        if (StrUtil.isBlank(blacklist)) {
            return;
        }
        for (String item : blacklist.split(",")) {
            String d = item.trim().toLowerCase();
            if (d.isEmpty()) {
                continue;
            }
            if (domain.equals(d) || domain.endsWith("." + d)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该邮箱域名暂不支持注册");
            }
        }
    }

    /**
     * 发送邮箱验证码（注册/登录通用；启用 Turnstile 时前端需带 token）
     */
    public void sendEmailCode(SendEmailCodeReq req) {
        turnstileService.verify(req.getTurnstileToken());
        FieldValidator.validateEmail(req.getEmail());
        // 60 秒冷却，防止刷验证码
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(EMAIL_CODE_FREQ_PREFIX + req.getEmail(), "1", FREQ_LIMIT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(ok)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码发送过于频繁，请稍后再试");
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(EMAIL_CODE_PREFIX + req.getEmail(), code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        emailService.sendCode(req.getEmail(), code);
    }

    /**
     * 邮箱验证码登录
     */
    public LoginResp emailCodeLogin(EmailCodeLoginReq req) {
        turnstileService.verify(req.getTurnstileToken());
        if (StrUtil.isBlank(req.getEmail()) || StrUtil.isBlank(req.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "邮箱和验证码不能为空");
        }
        String cacheKey = EMAIL_CODE_PREFIX + req.getEmail();
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码已过期，请重新发送");
        }
        if (!cachedCode.equals(req.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码错误");
        }
        redisTemplate.delete(cacheKey);

        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserEntity::getEmail, req.getEmail()));
        checkCanLogin(user);
        return doLogin(user);
    }

    /**
     * 忘记密码：通过邮箱验证码重置密码
     */
    public void forgotPassword(ForgotPasswordReq req) {
        turnstileService.verify(req.getTurnstileToken());
        FieldValidator.validateEmail(req.getEmail());
        FieldValidator.validatePassword(req.getNewPassword());
        if (StrUtil.isBlank(req.getEmailCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请输入邮箱验证码");
        }
        String cacheKey = EMAIL_CODE_PREFIX + req.getEmail();
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null || !cachedCode.equals(req.getEmailCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "邮箱验证码错误或已过期");
        }
        redisTemplate.delete(cacheKey);
        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserEntity::getEmail, req.getEmail()));
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该邮箱未注册");
        }
        checkCanLogin(user);
        user.setPasswordHash(BCrypt.hashpw(req.getNewPassword(), BCrypt.gensalt()));
        userMapper.update(user);
    }

    /**
     * 退出登录
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 当前登录用户信息
     */
    public UserProfileResp currentUserInfo() {
        Long userId = LoginUtil.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        return userService.getProfile(userId, userId);
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

    private UserEntity findByAccount(String account) {
        return userMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(UserEntity::getUsername).eq(account)
                        .or(UserEntity::getEmail).eq(account));
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
