package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户时间线（首页关注流）条目类型
 */
@Getter
@AllArgsConstructor
public enum UserTimelineItemTypeEnum {

    POST("post", "帖子");

    private final String code;
    private final String desc;
}
