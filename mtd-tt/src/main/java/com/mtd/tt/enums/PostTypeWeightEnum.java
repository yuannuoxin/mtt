package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子类型的基础热度权重。
 * <p>hot_score 初始值按此设置：原帖重要性最高，引用其次，回复再其次，转发最低。
 * 目的是让首页/搜索里回复、转发这类低价值内容天然分低、不占据太多展示位，
 * 而原帖即使互动少也能获得一定基础热度。后续互动（点赞/收藏等）在此基础上累加。
 */
@Getter
@AllArgsConstructor
public enum PostTypeWeightEnum {

    ORIGINAL("original", 1000L, "原创帖"),
    QUOTE("quote", 500L, "引用转发"),
    REPLY("reply", 120L, "回复"),
    REPOST("repost", 60L, "转发");

    private final String code;
    private final long base;
    private final String desc;

    public static PostTypeWeightEnum of(String code) {
        if (code == null) {
            return ORIGINAL;
        }
        for (PostTypeWeightEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return ORIGINAL;
    }
}
