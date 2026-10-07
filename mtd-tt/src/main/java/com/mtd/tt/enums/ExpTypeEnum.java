package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 经验获得类型：发帖最多，转发/引用/回复相对较少
 */
@Getter
@AllArgsConstructor
public enum ExpTypeEnum {

    POST("original", "发帖"),
    QUOTE("quote", "引用转发"),
    REPOST("repost", "转发"),
    REPLY("reply", "回复");

    private final String code;
    private final String desc;

    public static ExpTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (ExpTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
