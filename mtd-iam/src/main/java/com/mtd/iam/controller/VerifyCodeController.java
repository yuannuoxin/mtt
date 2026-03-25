package com.mtd.iam.controller;

import cn.hutool.core.util.ObjUtil;
import com.mtd.common.core.result.Result;
import com.mtd.iam.entity.LoginSession;
import com.mtd.iam.entity.UserContact;
import com.mtd.iam.enums.VerifyCodeEnum;
import com.mtd.iam.enums.YesEnum;
import com.mtd.iam.mapper.UserContactMapper;
import com.mtd.iam.req.SendSecondFactorVerifyCodeReq;
import com.mtd.iam.req.SendVerifyCodeRequest;
import com.mtd.iam.resp.SendVerifyCodeResp;
import com.mtd.iam.service.UserLoginSessionService;
import com.mtd.iam.service.UserVerifyCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "短信/邮箱 验证码")
@RestController
@RequestMapping("/verifyCode")
public class VerifyCodeController {

    @Autowired
    private UserVerifyCodeService userVerifyCodeService;

    @Autowired
    private UserContactMapper userContactMapper;

    @Autowired
    private UserLoginSessionService userLoginSessionService;

    /**
     * 发送二次验证验证码接口
     */
    @Operation(summary = "发送二次验证验证码", description = "根据联系方式 ID 发送短信或邮箱验证码（用于二次验证）")
    @PostMapping("/sendSecondFactorVerifyCode")
    public Result<SendVerifyCodeResp> sendSecondFactorVerifyCode(@RequestBody SendSecondFactorVerifyCodeReq req) {
        log.info("收到发送二次验证验证码请求，contactId: {}, sessionId: {}", req.getContactId(), req.getSessionId());

        LoginSession session = userLoginSessionService.getSession(req.getSessionId());

        if (ObjUtil.isNull(session)) {
            log.info("登录会话不存在或已过期，sessionId: {}", req.getSessionId());
            return Result.error("验证会话已过期，请重新登录");
        }

        // 1. 查询联系方式
        UserContact contact = userContactMapper.selectById(req.getContactId());
        if (ObjUtil.notEqual(contact.getUserId(), session.getUserId())) {
            log.info("联系方式不属于当前用户，contactId: {}, userId: {}", req.getContactId(), session.getUserId());
            return Result.error("该联系方式不属于当前用户");
        }
        if (ObjUtil.isNull(contact)) {
            log.info("联系方式不存在，contactId: {}", req.getContactId());
            return Result.error("联系方式不存在");
        }

        // 2. 验证联系方式是否属于当前用户且已验证
        if (!YesEnum.YES.getCode().equals(contact.getStatus()) || !YesEnum.YES.getCode().equals(contact.getVerified())) {
            log.info("联系方式状态异常，contactId: {}", req.getContactId());
            return Result.error("该联系方式不可用");
        }

        // 3. 构建发送验证码请求
        SendVerifyCodeRequest sendReq = new SendVerifyCodeRequest();
        sendReq.setType(contact.getContactType());
        sendReq.setReceive(contact.getContact());
        sendReq.setScene(com.mtd.iam.enums.VerifyCodeSceneEnum.LOGIN.getCode());
        if (VerifyCodeEnum.SMS.getCode().equals(contact.getContactType())) {
            sendReq.setCountryCode(contact.getCountryCode());
        }

        // 4. 发送验证码
        try {
            SendVerifyCodeResp resp = userVerifyCodeService.sendVerifyCode(sendReq);
            log.info("发送二次验证验证码成功，contactId: {}, type: {}", req.getContactId(), contact.getContactType());
            return Result.success(resp);
        } catch (Exception e) {
            log.error("发送二次验证验证码失败，contactId: {}", req.getContactId(), e);
            return Result.error("发送验证码失败：" + e.getMessage());
        }
    }

    /**
     * 发送验证码接口
     */
    @Operation(summary = "发送验证码", description = "发送短信或邮箱验证码")
    @PostMapping("/sendVerifyCode")
    public Result<SendVerifyCodeResp> sendVerifyCode(@RequestBody SendVerifyCodeRequest req) {
        log.info("收到发送验证码请求，类型：{}, 场景：{}, 接收者：{}", req.getType(), req.getScene(), req.getReceive());

        // 1. 参数校验
        if (!VerifyCodeEnum.SMS.getCode().equals(req.getType()) && !VerifyCodeEnum.EMAIL.getCode().equals(req.getType())) {
            log.info("验证码类型错误：{}", req.getType());
            return Result.error("验证码类型错误，仅支持 " + VerifyCodeEnum.SMS.getCode() + " 或 " + VerifyCodeEnum.EMAIL.getCode());
        }

        // 2. 发送验证码
        SendVerifyCodeResp sendVerifyCodeResp = userVerifyCodeService.sendVerifyCode(req);
        return Result.success(sendVerifyCodeResp);

    }

}
