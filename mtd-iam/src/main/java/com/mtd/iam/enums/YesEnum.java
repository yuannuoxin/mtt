package com.mtd.iam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum YesEnum {
    YES(1, "email"),
    NO(0, "sms"),

    ;
    private final Integer code;
    private final String desc;
}
