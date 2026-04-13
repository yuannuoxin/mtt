package com.mtd.st.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusEnum {
    DISABLED(0, "禁用"),
    ENABLED(1, "正常");

    private final Integer code;
    private final String desc;

    public static StatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (StatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
