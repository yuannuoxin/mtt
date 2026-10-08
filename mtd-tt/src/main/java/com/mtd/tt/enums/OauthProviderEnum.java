package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 第三方登录提供商枚举。当前支持 Google、GitHub、Telegram。
 */
@Getter
@AllArgsConstructor
public enum OauthProviderEnum {

    GOOGLE("google", "Google"),
    GITHUB("github", "GitHub"),
    TELEGRAM("telegram", "Telegram"),
    DISCORD("discord", "Discord");

    private final String code;
    private final String name;
}
