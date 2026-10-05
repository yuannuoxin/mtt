package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 帖子热度（hot_score）各互动类型的权重。
 * <p>hot_score = 发帖时的时间基准 + 后续互动加权累加，搜索按此倒序排序。
 * 数值越大代表互动越强、热度越高。浏览（view）因高频不参与 DB 加权，避免频繁写库。
 */
@Getter
@AllArgsConstructor
public enum HotScoreWeightEnum {

    LIKE(10L, "点赞"),
    BOOKMARK(15L, "收藏"),
    REPLY(20L, "回复"),
    REPOST(30L, "转发");

    private final long weight;
    private final String desc;
}
