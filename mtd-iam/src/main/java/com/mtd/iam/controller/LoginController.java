package com.mtd.iam.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mtd.common.core.result.Result;
import com.mtd.iam.constant.StrConstant;
import com.mtd.iam.entity.User;
import com.mtd.iam.entity.UserContact;
import com.mtd.iam.entity.UserVerifyCode;
import com.mtd.iam.enums.VerifyCodeEnum;
import com.mtd.iam.enums.VerifyCodeSceneEnum;
import com.mtd.iam.enums.YesEnum;
import com.mtd.iam.mapper.UserContactMapper;
import com.mtd.iam.mapper.UserMapper;
import com.mtd.iam.req.VerifyCodeLoginReq;
import com.mtd.iam.service.UserVerifyCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
