package com.mtd.iam.controller;

import com.mtd.common.core.result.Result;
import com.mtd.iam.enums.VerifyCodeEnum;
import com.mtd.iam.req.SendVerifyCodeRequest;
import com.mtd.iam.resp.SendVerifyCodeResp;
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

    /**
     * 发送验证码接口
     */
    @Operation(summary = "发送验证码", description = "发送短信或邮箱验证码")
    @PostMapping("/verify-code/send")
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
