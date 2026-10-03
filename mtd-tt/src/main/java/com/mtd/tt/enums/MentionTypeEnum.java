package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @提及类型
 */
@Getter
@AllArgsConstructor
public enum MentionTypeEnum {

    USER("user", "@用户"),
    TOPIC("topic", "话题");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "user/topic";

    public static MentionTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (MentionTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
