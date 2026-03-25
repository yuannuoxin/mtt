package com.mtd.iam.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mtd.common.core.result.Result;
import com.mtd.iam.constant.StrConstant;
import com.mtd.iam.entity.User;
import com.mtd.iam.entity.UserContact;
import com.mtd.iam.enums.VerifyCodeEnum;
import com.mtd.iam.enums.VerifyCodeSceneEnum;
import com.mtd.iam.enums.YesEnum;
import com.mtd.iam.mapper.UserContactMapper;
import com.mtd.iam.mapper.UserMapper;
import com.mtd.iam.req.EmailRegisterRequest;
import com.mtd.iam.req.PhoneRegisterRequest;
import com.mtd.iam.service.UserVerifyCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户注册控制器
 */
@Slf4j
@Tag(name = "用户注册", description = "邮箱注册相关接口")
@RestController
@RequestMapping("/auth")
public class RegisterController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserContactMapper userContactMapper;


    @Autowired
    private UserVerifyCodeService userVerifyCodeService;

    /**
     * 邮箱注册接口
     */
    @Operation(summary = "邮箱注册", description = "通过邮箱验证码完成用户注册")
    @PostMapping("/register/email")
    public Result<SaTokenInfo> registerByEmail(@RequestBody EmailRegisterRequest req) {
        log.info("收到邮箱注册请求，邮箱：{}", req.getEmail());

        // 1. 验证验证码
        boolean codeValid = userVerifyCodeService.verifyCode(req.getVerifyId(), req.getEmail(), req.getCode(), VerifyCodeSceneEnum.REGISTER.getCode());
        if (!codeValid) {
            return Result.error("验证码错误或已过期");
        }

        // 2. 检查邮箱是否已注册
        UserContact existingContact = userContactMapper.selectOne(
                new LambdaQueryWrapper<UserContact>()
                        .eq(UserContact::getContact, req.getEmail())
                        .eq(UserContact::getContactType, VerifyCodeEnum.EMAIL.getCode())
                        .eq(UserContact::getMajor, YesEnum.YES.getCode())
                        .last(StrConstant.LIMIT_1)
        );

        if (existingContact != null) {
            log.info("邮箱已被注册：{}", req.getEmail());
            return Result.error("该邮箱已被注册");
        }

        // 3. 创建用户
        User user = new User();
        String nickname = StrUtil.subBefore(req.getEmail(), "@", false);
        user.setNickname(nickname);
        user.setStatus(YesEnum.YES.getCode()); // 正常状态
        userMapper.insert(user);

        log.info("创建用户成功，userId: {}", user.getId());

        // 5. 创建联系方式
        UserContact contact = new UserContact();
        contact.setUserId(user.getId());
        contact.setContactType(VerifyCodeEnum.EMAIL.getCode());
        contact.setContact(req.getEmail());
        contact.setMajor(YesEnum.YES.getCode()); // 主账号
        contact.setVerified(YesEnum.YES.getCode()); // 已验证
        contact.setStatus(YesEnum.YES.getCode());
        userContactMapper.insert(contact);

        log.info("创建用户联系方式成功，userId: {}", user.getId());

        // 6. 标记验证码为已使用
        userVerifyCodeService.useVerifyCode(req.getVerifyId());

        // 7. 返回结果
        log.info("用户注册完成，userId: {}, email: {}", user.getId(), req.getEmail());

        StpUtil.login(user.getId());
        return Result.success(StpUtil.getTokenInfo());
    }


    /**
     * 手机号注册接口
     */
    @Operation(summary = "手机号注册", description = "通过手机号验证码完成用户注册")
    @PostMapping("/register/phone")
    public Result<SaTokenInfo> registerByPhone(@RequestBody PhoneRegisterRequest req) {
        log.info("收到手机号注册请求，手机号：{}", req.getPhone());

        // 1. 验证验证码
        boolean codeValid = userVerifyCodeService.verifySmsCode(req.getVerifyId(), req.getCountryCode(), req.getPhone(), req.getCode(), VerifyCodeSceneEnum.REGISTER.getCode());
        if (!codeValid) {
            return Result.error("验证码错误或已过期");
        }

        // 2. 检查手机号是否已注册
        UserContact existingContact = userContactMapper.selectOne(
                new LambdaQueryWrapper<UserContact>()
                        .eq(UserContact::getCountryCode, req.getCountryCode())
                        .eq(UserContact::getContact, req.getPhone())
                        .eq(UserContact::getContactType, VerifyCodeEnum.SMS.getCode())
                        .eq(UserContact::getMajor, YesEnum.YES.getCode())
                        .last(StrConstant.LIMIT_1)
        );

        if (existingContact != null) {
            log.info("手机号已被注册：{} countryCode：{}", req.getPhone(), req.getCountryCode());
            return Result.error("该手机号已被注册");
        }

        // 3. 创建用户
        User user = new User();
        String nickname = DesensitizedUtil.mobilePhone(req.getPhone());
        user.setNickname(nickname);
        user.setStatus(YesEnum.YES.getCode()); // 正常状态
        userMapper.insert(user);

        log.info("创建用户成功，userId: {}", user.getId());

        // 5. 创建联系方式
        UserContact contact = new UserContact();
        contact.setUserId(user.getId());
        contact.setContactType(VerifyCodeEnum.SMS.getCode());
        contact.setCountryCode(req.getCountryCode());
        contact.setContact(req.getPhone());
        contact.setMajor(YesEnum.YES.getCode()); // 主账号
        contact.setVerified(YesEnum.YES.getCode()); // 已验证
        contact.setStatus(YesEnum.YES.getCode());
        userContactMapper.insert(contact);

        log.info("创建用户联系方式成功，userId: {}", user.getId());

        // 6. 标记验证码为已使用
        userVerifyCodeService.useVerifyCode(req.getVerifyId());

        // 7. 返回结果
        log.info("用户注册完成，userId: {}, email: {}", user.getId(), req.getPhone());

        StpUtil.login(user.getId());
        return Result.success(StpUtil.getTokenInfo());
    }
}
