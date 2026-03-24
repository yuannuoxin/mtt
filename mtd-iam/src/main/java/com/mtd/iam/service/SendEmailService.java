package com.mtd.iam.service;

import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SendEmailService {

    @Value("${mail.host:}")
    private String mailHost;

    @Value("${mail.port:25}")
    private Integer mailPort;

    @Value("${mail.username:}")
    private String mailUsername;

    @Value("${mail.password:}")
    private String mailPassword;

    @Value("${mail.from:}")
    private String mailFrom;


    /**
     * 发送邮件验证码
     */
    public void sendEmailCode(String email, String code) {
        // 检查邮件配置
        if (StringUtils.isBlank(mailHost) || StringUtils.isBlank(mailUsername) || StringUtils.isBlank(mailPassword)) {
            log.error("邮件服务器配置不完整");
            return;
        }

        // 配置邮件账户
        MailAccount account = new MailAccount();
        account.setHost(mailHost);
        account.setPort(mailPort);
        account.setAuth(true);
        account.setFrom(mailFrom);
        account.setUser(mailUsername);
        account.setPass(mailPassword);

        // 构建邮件内容
        String subject = "验证码";
        String content = String.format(
                "您的验证码是：<b style='font-size: 24px; color: #e74c3c;'>%s</b><br/>" +
                        "该验证码 5 分钟内有效，请勿泄露给他人。\n\n" +
                        "如非本人操作，请忽略此邮件。",
                code
        );

        // 发送邮件
        MailUtil.send(account, email, subject, content, true);
        log.info("邮件验证码发送成功，邮箱：{}", email);

    }
}
