package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    LIKE("like", "点赞"),
    COMMENT("comment", "评论"),
    FOLLOW("follow", "关注"),
    REPOST("repost", "转发"),
    MENTION("mention", "@提及");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "like/comment/follow/repost/mention";

    public static NotificationTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (NotificationTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
