package com.mtd.iam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VerifyCodeEnum {
    EMAIL("email"),
    SMS("sms"),

    ;
    private final String code;
}
