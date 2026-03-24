package com.mtd.iam.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mtd.iam.entity.UserVerifyCode;
import com.mtd.iam.enums.VerifyCodeEnum;
import com.mtd.iam.mapper.UserVerifyCodeMapper;
import com.mtd.iam.req.SendVerifyCodeRequest;
import com.mtd.iam.resp.SendVerifyCodeResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
public class UserVerifyCodeService extends ServiceImpl<UserVerifyCodeMapper, UserVerifyCode> {

    @Autowired
    private UserVerifyCodeMapper userVerifyCodeMapper;

    @Autowired
    private SendEmailService sendEmailService;


    /**
     * 发送验证码并返回 verifyId（支持手机号国家区号）
     */
    public SendVerifyCodeResp sendVerifyCode(SendVerifyCodeRequest req) {
        // 1. 生成 6 位数字验证码
        String code = RandomUtil.randomNumbers(6);

        // 2. 保存验证码到数据库
        UserVerifyCode verifyCode = new UserVerifyCode();
        verifyCode.setCountryCode(req.getCountryCode());
        verifyCode.setReceive(req.getReceive());
        verifyCode.setCode(code);
        verifyCode.setType(req.getType());
        verifyCode.setScene(req.getScene());
        verifyCode.setUsed(0);
        userVerifyCodeMapper.insert(verifyCode);

        log.info("验证码已保存，verifyId: {}, code: {}", verifyCode.getId(), code);

        // 3. 发送验证码
        if (VerifyCodeEnum.EMAIL.getCode().equals(req.getType())) {
            sendEmailService.sendEmailCode(req.getReceive(), code);
        } else {
            // TODO: 实现短信发送
            log.info("短信验证码：{}, 手机号：{}{}", code, req.getCountryCode(), req.getReceive());
        }

        SendVerifyCodeResp sendVerifyCodeResp = new SendVerifyCodeResp();
        sendVerifyCodeResp.setId(verifyCode.getId());
        //todo 
        sendVerifyCodeResp.setCode(verifyCode.getCode());
        log.info("验证码发送成功，verifyId: {}", verifyCode.getId());
        return sendVerifyCodeResp;
    }


    public boolean verifySmsCode(Long verifyId, String countryCode, String receive, String code, String scene) {
        UserVerifyCode verifyCode = userVerifyCodeMapper.selectById(verifyId);

        if (verifyCode == null) {
            return false;
        }
        if (ObjUtil.notEqual(verifyCode.getCode(), code) ||
                ObjUtil.notEqual(verifyCode.getReceive(), receive) ||
                ObjUtil.notEqual(verifyCode.getCountryCode(), countryCode)) {
            return false;
        }

        // 检查验证码是否过期（5 分钟有效期）
        return Duration.between(verifyCode.getCreateTime(), LocalDateTime.now()).toMinutes() < 5;
    }

    public boolean verifyCode(Long verifyId, String receive, String code, String scene) {
        UserVerifyCode verifyCode = userVerifyCodeMapper.selectById(verifyId);

        if (verifyCode == null) {
            return false;
        }
        if (ObjUtil.notEqual(verifyCode.getCode(), code) || ObjUtil.notEqual(verifyCode.getReceive(), receive)) {
            return false;
        }

        // 检查验证码是否过期（假设验证码 5 分钟有效期）
        return Duration.between(verifyCode.getCreateTime(), LocalDateTime.now()).toMinutes() < 5;
    }

    public void useVerifyCode(Long verifyId) {
        UserVerifyCode verifyCode = userVerifyCodeMapper.selectById(verifyId);

        if (verifyCode == null) {
            return;
        }
        verifyCode.setUsed(verifyCode.getUsed() + 1);
        userVerifyCodeMapper.updateById(verifyCode);
    }
}
