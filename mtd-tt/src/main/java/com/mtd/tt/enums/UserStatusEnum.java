package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    ACTIVE("active", "正常"),
    FROZEN("frozen", "临时冻结"),
    BANNED("banned", "永久封禁");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "active/frozen/banned";

    public static UserStatusEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (UserStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
