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
import com.mtd.tt.model.req.PasswordLoginReq;
import com.mtd.tt.model.req.RegisterReq;
import com.mtd.tt.model.resp.LoginResp;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mtd.tt.enums.UserStatusEnum;
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

    private static final String EMAIL_CODE_PREFIX = "tt:login:email:code:";
    private static final long CODE_EXPIRE_MINUTES = 5;

    /**
     * 密码登录（用户名或邮箱 + 密码）
     */
    public LoginResp passwordLogin(PasswordLoginReq req) {
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
        if (StrUtil.isBlank(req.getUsername()) || StrUtil.isBlank(req.getNickname())
                || StrUtil.isBlank(req.getEmail()) || StrUtil.isBlank(req.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名、昵称、邮箱、密码均不能为空");
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
        userMapper.insertSelective(user);

        return doLogin(user);
    }

    /**
     * 发送邮箱验证码
     */
    public void sendEmailCode(String email) {
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "邮箱不能为空");
        }
        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create().eq(UserEntity::getEmail, email));
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该邮箱未注册");
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(EMAIL_CODE_PREFIX + email, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("邮箱验证码: {}, 邮箱: {}", code, email);
    }

    /**
     * 邮箱验证码登录
     */
    public LoginResp emailCodeLogin(EmailCodeLoginReq req) {
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
     * 退出登录
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 当前登录用户信息
     */
    public UserProfileResp currentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
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
