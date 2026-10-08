package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 第三方登录提供商枚举。当前仅支持 Google，后续可扩展 github 等。
 */
@Getter
@AllArgsConstructor
public enum OauthProviderEnum {

    GOOGLE("google", "Google");

    private final String code;
    private final String name;
}
