package com.mtd.tt.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务：用于注册/登录邮箱验证码。
 * <p>是否真正发信由 sys_config 的 email_code.enabled 控制；开发期默认关闭，
 * 此时只打印验证码到日志，方便本地调试。SMTP 参数从 sys_config 读取。</p>
 */
@Slf4j
@Service
public class EmailService {

    @Autowired
    private SysConfigService configService;

    public boolean isEnabled() {
        return configService.getBool("email_code.enabled", false);
    }

    /**
     * 发送邮箱验证码；未启用邮件或 SMTP 未配置时降级为日志输出（不阻塞流程）。
     */
    public void sendCode(String to, String code) {
        if (!isEnabled()) {
            log.info("[邮箱验证码][邮件服务未启用，仅记录] {} -> {}", code, to);
            return;
        }
        String host = StrUtil.nullToEmpty(configService.get("email.smtp.host"));
        if (StrUtil.isBlank(host)) {
            log.warn("[邮箱验证码][SMTP 未配置，降级日志] {} -> {}", code, to);
            return;
        }
        String port = StrUtil.nullToEmpty(configService.get("email.smtp.port"));
        String username = StrUtil.nullToEmpty(configService.get("email.smtp.username"));
        String password = StrUtil.nullToEmpty(configService.get("email.smtp.password"));
        String from = StrUtil.nullToEmpty(configService.get("email.smtp.from"));
        try {
            String content = "您的验证码是：" + code + "，5 分钟内有效，请勿泄露给他人。";
            if (StrUtil.isBlank(from)) {
                from = username;
            }
            MailAccount account = new MailAccount();
            account.setHost(host);
            account.setPort(parsePort(port));
            account.setAuth(true);
            account.setSslEnable(true);
            account.setFrom(from);
            account.setUser(username);
            account.setPass(password);
            MailUtil.send(account, to, "【MTT】邮箱验证码", content, false);
            log.info("[邮箱验证码] 已发送到 {}", to);
        } catch (Exception e) {
            log.error("[邮箱验证码] 发送失败: {}", e.getMessage(), e);
        }
    }

    private int parsePort(String port) {
        try {
            return Integer.parseInt(StrUtil.isBlank(port) ? "465" : port);
        } catch (NumberFormatException e) {
            return 465;
        }
    }
}
