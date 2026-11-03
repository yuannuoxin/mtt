package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录来源方式：用于登录日志记录的 provider 字段
 */
@Getter
@AllArgsConstructor
public enum LoginProviderEnum {

    PASSWORD("password", "密码登录"),
    EMAIL_CODE("email_code", "邮箱验证码登录"),
    REGISTER("register", "注册"),
    GOOGLE("google", "Google"),
    GITHUB("github", "GitHub"),
    TELEGRAM("telegram", "Telegram"),
    DISCORD("discord", "Discord");

    private final String code;
    private final String desc;

    public static LoginProviderEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (LoginProviderEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
