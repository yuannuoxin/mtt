package com.mtd.iam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VerifyCodeSceneEnum {
    /**
     * login/register/reset_pwd/bind
     */
    LOGIN("login"),
    REGISTER("register"),
    RESET_PWD("reset_pwd"),
    BIND("bind"),

    ;
    private final String code;
}
