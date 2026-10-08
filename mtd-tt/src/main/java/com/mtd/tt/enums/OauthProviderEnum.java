package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 第三方登录提供商枚举。当前支持 Google、GitHub。
 */
@Getter
@AllArgsConstructor
public enum OauthProviderEnum {

    GOOGLE("google", "Google"),
    GITHUB("github", "GitHub");

    private final String code;
    private final String name;
}
