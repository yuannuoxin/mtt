package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建帖子-@提及项
 */
@Data
public class CreatePostMentionReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Parameter(description = "提及类型，可选值：user/topic（见 MentionTypeEnum）")
    private String mentionType;
    @Parameter(description = "目标ID（user则user_id，topic则topic_id）")
    private Long targetId;
    @Parameter(description = "在正文中的起始下标（含）")
    private Long startIdx;
    @Parameter(description = "在正文中的结束下标（不含）")
    private Long endIdx;
}
