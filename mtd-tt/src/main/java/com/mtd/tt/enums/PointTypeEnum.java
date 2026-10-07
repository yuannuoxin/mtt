package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 积分变更类型
 */
@Getter
@AllArgsConstructor
public enum PointTypeEnum {

    CHECKIN("checkin", "每日签到"),
    POST("post", "发帖"),
    QUOTE("quote", "引用"),
    REPOST("repost", "转发"),
    REPLY("reply", "回复"),
    UPLOAD("upload", "上传文件"),
    REGISTER("register", "注册赠送");

    private final String code;
    private final String desc;

    public static PointTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (PointTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
