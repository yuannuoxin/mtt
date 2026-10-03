package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子可见性
 */
@Getter
@AllArgsConstructor
public enum PostVisibilityEnum {

    PUBLIC("public", "所有人可见"),
    FOLLOWERS_ONLY("followers_only", "仅关注者可见");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "public/followers_only";

    public static PostVisibilityEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (PostVisibilityEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
