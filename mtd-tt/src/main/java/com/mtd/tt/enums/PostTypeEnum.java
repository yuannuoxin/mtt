package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子类型
 */
@Getter
@AllArgsConstructor
public enum PostTypeEnum {

    ORIGINAL("original", "原创帖"),
    REPLY("reply", "回复"),
    QUOTE("quote", "引用转发"),
    REPOST("repost", "转发");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "original/reply/quote/repost";

    public static PostTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (PostTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
