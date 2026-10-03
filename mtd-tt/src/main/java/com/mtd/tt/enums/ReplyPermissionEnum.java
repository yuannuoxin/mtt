package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子回复权限
 */
@Getter
@AllArgsConstructor
public enum ReplyPermissionEnum {

    ANYONE("anyone", "任何人可回复"),
    FOLLOWED("followed", "仅关注者可回复"),
    MENTIONED("mentioned", "仅被@用户可回复");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "anyone/followed/mentioned";

    public static ReplyPermissionEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (ReplyPermissionEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
