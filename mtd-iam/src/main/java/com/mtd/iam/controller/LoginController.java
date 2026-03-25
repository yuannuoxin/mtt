package com.mtd.iam.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mtd.common.core.result.Result;
import com.mtd.iam.constant.StrConstant;
import com.mtd.iam.entity.*;
import com.mtd.iam.enums.SecondFactorTypeEnum;
import com.mtd.iam.enums.VerifyCodeEnum;
import com.mtd.iam.enums.VerifyCodeSceneEnum;
import com.mtd.iam.enums.YesEnum;
import com.mtd.iam.mapper.UserContactMapper;
import com.mtd.iam.mapper.UserMapper;
import com.mtd.iam.mapper.UserMfaMapper;
import com.mtd.iam.req.GetSecondFactorContactsReq;
import com.mtd.iam.req.PasswordLoginRequest;
import com.mtd.iam.req.SecondFactorVerifyRequest;
import com.mtd.iam.req.VerifyCodeLoginReq;
import com.mtd.iam.resp.LoginResp;
import com.mtd.iam.resp.SecondFactorOption;
import com.mtd.iam.service.UserCredentialService;
import com.mtd.iam.service.UserLoginSessionService;
import com.mtd.iam.service.UserVerifyCodeService;
import com.mtd.iam.util.TotpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "登录")
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserVerifyCodeService userVerifyCodeService;

    @Autowired
    private UserContactMapper userContactMapper;

    @Autowired
    private UserCredentialService userCredentialService;

    @Autowired
    private UserLoginSessionService userLoginSessionService;

    @Autowired
    private UserMfaMapper userMfaMapper;

    @GetMapping("/tokenInfo")
    public Result<SaTokenInfo> tokenInfo() {
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return Result.success(tokenInfo);
    }

    @GetMapping("/isLogin")
    public Result<Boolean> isLogin() {
        boolean isLogin = StpUtil.isLogin();
        return Result.success(isLogin);
    }


    @Operation(summary = "验证码登录")
    @PostMapping("/verifyCodeLogin")
    public Result<SaTokenInfo> sendVerifyCode(@RequestBody VerifyCodeLoginReq req) {
        // 1. 验证验证码
        boolean codeValid = userVerifyCodeService.verifyLoginCode(req.getId(), req.getCode(), VerifyCodeSceneEnum.LOGIN.getCode());
        if (!codeValid) {
            return Result.error("验证码错误或已过期");
        }
        UserVerifyCode userVerifyCode = userVerifyCodeService.getById(req.getId());
        UserContact userContact = userContactMapper.selectOne(Wrappers.<UserContact>lambdaQuery()
                .eq(UserContact::getMajor, YesEnum.YES.getCode())
                .eq(UserContact::getStatus, YesEnum.YES.getCode())
                .eq(UserContact::getVerified, YesEnum.YES.getCode())
                .eq(UserContact::getContact, userVerifyCode.getReceive())
                .eq(UserContact::getContactType, userVerifyCode.getType())
                .eq(ObjUtil.equals(userVerifyCode.getType(), VerifyCodeEnum.SMS.getCode()), UserContact::getCountryCode, userVerifyCode.getCountryCode())
                .orderByDesc(UserContact::getId)
                .last(StrConstant.LIMIT_1)
        );
        if (ObjUtil.isNull(userContact)) {
            return Result.error("未注册或者用户状态不可登录");
        }
        User user = userMapper.selectById(userContact.getUserId());

        if (ObjUtil.isNull(user)) {
            return Result.error("未注册或者用户状态不可登录");
        }
        StpUtil.login(user.getId());
        userVerifyCodeService.useVerifyCode(req.getId());

        return Result.success(StpUtil.getTokenInfo());

    }

    /**
     * 密码登录接口（支持账号/邮箱/手机号）
     */
    @Operation(summary = "密码登录", description = "支持用户名、邮箱、手机号 + 密码登录，可能需要二次验证")
    @PostMapping("/passwordLogin")
    public Result<LoginResp> passwordLogin(@Valid @RequestBody PasswordLoginRequest req) {
        log.info("收到密码登录请求，account: {}", req.getAccount());

        // 1. 查询用户
        User user = findUserByAccount(req.getAccount(), req.getCountryCode());
        if (ObjUtil.isNull(user)) {
            log.info("用户不存在，account: {}", req.getAccount());
            return Result.error("用户不存在或账号密码错误");
        }

        // 2. 检查用户状态
        if (!YesEnum.YES.getCode().equals(user.getStatus())) {
            log.info("用户状态异常，userId: {}, status: {}", user.getId(), user.getStatus());
            return Result.error("用户状态异常，无法登录");
        }

        // 3. 验证密码
        boolean passwordValid = userCredentialService.verifyPassword(user, req.getPassword());
        if (!passwordValid) {
            log.info("密码验证失败，userId: {}", user.getId());
            return Result.error("用户不存在或账号密码错误");
        }

        // 4. 检查用户是否配置了二次验证
        List<String> supportedTypes = getSupportedSecondFactorTypes(user);
        boolean needSecondFactor = !supportedTypes.isEmpty();

        if (needSecondFactor) {
            // 用户已配置二次验证，创建会话存储 userId（不需要存储 secondFactorType，前端会自己选择）
            String sessionId = userLoginSessionService.createTempSession(user.getId());
            log.info("用户已配置二次验证，userId: {}, 可用类型数量：{}", user.getId(), supportedTypes.size());

            LoginResp resp = new LoginResp();
            resp.setNeedSecondFactor(true);
            resp.setSecondFactorId(sessionId); // sessionId，用于后续二次验证
            resp.setSupportedFactorTypes(supportedTypes); // 支持的验证类型列表，前端根据此列表展示选项
            resp.setSecondFactorDesc("请选择一种二次验证方式");
            return Result.success(resp);
        }

        // 用户未配置二次验证，直接登录成功
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        LoginResp resp = LoginResp.createLoginSuccess(tokenInfo);

        log.info("密码登录成功，userId: {}", user.getId());
        return Result.success(resp);
    }

    /**
     * 获取可用的二次验证联系方式列表
     */
    @Operation(summary = "获取二次验证联系方式", description = "根据登录时返回的验证类型，获取对应的可用联系方式列表（带 ID）")
    @PostMapping("/getSecondFactorContacts")
    public Result<List<SecondFactorOption>> getSecondFactorContacts(@Valid @RequestBody GetSecondFactorContactsReq req) {
        log.info("获取二次验证联系方式，sessionId: {}, type: {}", req.getSessionId(), req.getSecondFactorType());

        // 1. 验证会话
        LoginSession session = userLoginSessionService.getSession(req.getSessionId());
        if (ObjUtil.isNull(session)) {
            log.info("登录会话不存在或已过期，sessionId: {}", req.getSessionId());
            return Result.error("验证会话已过期，请重新登录");
        }


        List<SecondFactorOption> contacts = userContactMapper.selectList(
                        Wrappers.<UserContact>lambdaQuery()
                                .eq(UserContact::getUserId, session.getUserId())
                                .eq(UserContact::getStatus, YesEnum.YES.getCode())
                                .eq(UserContact::getVerified, YesEnum.YES.getCode())
                                .eq(UserContact::getContactType, req.getSecondFactorType()))
                .stream()
                .map(contact -> new SecondFactorOption(
                        contact.getId(),
                        contact.getContactType(),
                        ObjUtil.equals(contact.getContactType(), VerifyCodeEnum.EMAIL.getCode()) ? SecondFactorTypeEnum.EMAIL.getDesc() : SecondFactorTypeEnum.SMS.getDesc(),
                        ObjUtil.equals(contact.getContactType(), VerifyCodeEnum.EMAIL.getCode()) ? DesensitizedUtil.email(contact.getContact()) : DesensitizedUtil.mobilePhone(contact.getContact()),
                        contact.getCountryCode()
                ))
                .collect(Collectors.toList());


        if (contacts.isEmpty()) {
            log.info("没有可用的联系方式，userId: {}, type: {}", session.getUserId(), req.getSecondFactorType());
            return Result.error("没有可用的验证方式");
        }

        log.info("获取到{}个联系方式，userId: {}, type: {}", contacts.size(), session.getUserId(), req.getSecondFactorType());
        return Result.success(contacts);
    }

    /**
     * 二次验证接口（MFA/邮箱验证码/短信验证码）
     */
    @Operation(summary = "二次验证")
    @PostMapping("/secondFactorVerify")
    public Result<LoginResp> secondFactorVerify(@Valid @RequestBody SecondFactorVerifyRequest req) {
        log.info("收到二次验证请求，verifyId: {}, type: {}", req.getSessionId(), req.getSecondFactorType());

        // 1. 获取登录会话
        String sessionId = req.getSessionId();
        LoginSession session = userLoginSessionService.getSession(sessionId);

        if (ObjUtil.isNull(session)) {
            log.info("登录会话不存在或已过期，sessionId: {}", sessionId);
            return Result.error("验证会话已过期，请重新登录");
        }
        String secondFactorType = req.getSecondFactorType();

        // 3. 根据验证类型进行验证
        boolean verified = false;

        if (SecondFactorTypeEnum.MFA.getType().equals(secondFactorType)) {
            // 验证 MFA 动态码
            verified = verifyMfaCode(session.getUserId(), req.getCode());
        } else if (SecondFactorTypeEnum.EMAIL.getType().equals(secondFactorType) || SecondFactorTypeEnum.SMS.getType().equals(secondFactorType)) {
            // 验证邮箱/短信验证码（需要指定联系方式）
            verified = verifyEmailOrSmsCodeWithContact(session.getUserId(), req.getCode(), secondFactorType, req.getVerifyCodeId());
        }

        if (!verified) {
            log.info("二次验证失败，userId: {}, type: {}", session.getUserId(), secondFactorType);
            return Result.error("验证码错误");
        }

        // 4. 验证成功，删除会话并登录
        userLoginSessionService.deleteSession(sessionId);
        StpUtil.login(session.getUserId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        LoginResp resp = LoginResp.createLoginSuccess(tokenInfo);

        log.info("二次验证成功，userId: {}", session.getUserId());
        return Result.success(resp);
    }

    /**
     * 验证 MFA 动态码
     */
    private boolean verifyMfaCode(Long userId, String code) {
        try {
            // 查询用户的 MFA 信息
            UserMfa userMfa = userMfaMapper.selectOne(
                    Wrappers.<UserMfa>lambdaQuery()
                            .eq(UserMfa::getUserId, userId)
                            .eq(UserMfa::getStatus, YesEnum.YES.getCode())
                            .orderByDesc(UserMfa::getId)
                            .last(StrConstant.LIMIT_1)
            );

            if (ObjUtil.isNull(userMfa) || ObjUtil.isEmpty(userMfa.getSecret())) {
                log.warn("用户未启用 MFA，userId: {}", userId);
                return false;
            }

            // 验证 TOTP 动态码
            boolean isValid = TotpUtil.verifyCode(userMfa.getSecret(), code);
            log.debug("MFA 验证结果：{}, userId: {}", isValid, userId);
            return isValid;
        } catch (Exception e) {
            log.error("MFA 验证异常，userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 验证邮箱/短信验证码（指定联系方式）
     */
    private boolean verifyEmailOrSmsCodeWithContact(Long userId, String code, String type, Long verifyCodeId) {

        UserVerifyCode verifyCode = userVerifyCodeService.getById(verifyCodeId);

        if (ObjUtil.isNull(verifyCode)) {
            log.warn("未找到有效的验证码，userId: {}, verifyCodeId: {}", userId, verifyCodeId);
            return false;
        }

        // 验证验证码
        boolean isValid = userVerifyCodeService.verifyCode(
                verifyCode.getId(),
                verifyCode.getReceive(),
                code,
                VerifyCodeSceneEnum.LOGIN.getCode()
        );

        if (isValid) {
            // 标记验证码为已使用
            userVerifyCodeService.useVerifyCode(verifyCode.getId());
            log.info("验证码验证成功，verifyCodeId: {}, type: {}", verifyCodeId, type);
            return true;
        }

        log.warn("验证码验证失败，userId: {}, verifyCodeId: {}", userId, verifyCodeId);
        return false;

    }

    /**
     * 根据账号查找用户
     */
    private User findUserByAccount(String account, String countryCode) {
        // 判断账号类型
        if (StrUtil.contains(account, "@")) {
            // 邮箱登录
            return findUserByEmail(account);
        } else if (StringUtils.isNotBlank(countryCode)) {
            // 手机号登录
            return findUserByPhone(account, countryCode);
        } else {
            // 用户名登录
            return findUserByUsername(account);
        }
    }

    /**
     * 获取用户支持的所有二次验证类型
     */
    private List<String> getSupportedSecondFactorTypes(User user) {
        List<String> types = new java.util.ArrayList<>();

        // 1. 检查 MFA
        if (userCredentialService.isUserMfaEnabled(user)) {
            types.add(SecondFactorTypeEnum.MFA.getType());
        }

        // 2. 查询所有已验证的联系方式（包括主账号和辅助账号）
        List<UserContact> contacts = userContactMapper.selectList(
                Wrappers.<UserContact>lambdaQuery()
                        .eq(UserContact::getUserId, user.getId())
                        .eq(UserContact::getStatus, YesEnum.YES.getCode())
                        .eq(UserContact::getVerified, YesEnum.YES.getCode())
        );

        boolean hasEmail = false;
        boolean hasSms = false;

        for (UserContact contact : contacts) {
            if (!hasEmail && VerifyCodeEnum.EMAIL.getCode().equals(contact.getContactType())) {
                types.add(SecondFactorTypeEnum.EMAIL.getType());
                hasEmail = true;
            }
            if (!hasSms && VerifyCodeEnum.SMS.getCode().equals(contact.getContactType())) {
                types.add(SecondFactorTypeEnum.SMS.getType());
                hasSms = true;
            }

            if (hasEmail && hasSms) {
                break;
            }
        }

        return types;
    }

    /**
     * 根据邮箱查找用户
     */
    private User findUserByEmail(String email) {
        UserContact contact = userContactMapper.selectOne(
                Wrappers.<UserContact>lambdaQuery()
                        .eq(UserContact::getContact, email)
                        .eq(UserContact::getContactType, VerifyCodeEnum.EMAIL.getCode())
                        .eq(UserContact::getMajor, YesEnum.YES.getCode())
                        .eq(UserContact::getStatus, YesEnum.YES.getCode())
                        .eq(UserContact::getVerified, YesEnum.YES.getCode())
                        .orderByDesc(UserContact::getId)
                        .last(StrConstant.LIMIT_1)
        );

        if (ObjUtil.isNull(contact)) {
            return null;
        }

        return userMapper.selectById(contact.getUserId());
    }

    /**
     * 根据手机号查找用户
     */
    private User findUserByPhone(String phone, String countryCode) {
        UserContact contact = userContactMapper.selectOne(
                Wrappers.<UserContact>lambdaQuery()
                        .eq(UserContact::getContact, phone)
                        .eq(UserContact::getCountryCode, countryCode)
                        .eq(UserContact::getContactType, VerifyCodeEnum.SMS.getCode())
                        .eq(UserContact::getMajor, YesEnum.YES.getCode())
                        .eq(UserContact::getStatus, YesEnum.YES.getCode())
                        .eq(UserContact::getVerified, YesEnum.YES.getCode())
                        .orderByDesc(UserContact::getId)
                        .last(StrConstant.LIMIT_1)
        );

        if (ObjUtil.isNull(contact)) {
            return null;
        }

        return userMapper.selectById(contact.getUserId());
    }

    /**
     * 根据用户名查找用户
     */
    private User findUserByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username)
                        .eq(User::getStatus, YesEnum.YES.getCode())
                        .orderByDesc(User::getId)
                        .last(StrConstant.LIMIT_1)
        );
    }
}
