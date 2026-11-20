package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子互动类型：讨论/问答/投票/悬赏/抽奖
 * 与结构性 type（original/reply/quote/repost）相互独立。
 */
@Getter
@AllArgsConstructor
public enum PostKindEnum {

    DISCUSSION("discussion", "讨论"),
    QUESTION("question", "问答"),
    POLL("poll", "投票"),
    BOUNTY("bounty", "悬赏"),
    LOTTERY("lottery", "抽奖");

    private final String code;
    private final String desc;

    public static PostKindEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (PostKindEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
