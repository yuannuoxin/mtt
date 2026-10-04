package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;

/**
 * 帖子@提及响应
 */
@Data
public class PostMentionResp implements Serializable {

    @Parameter(description = "提及ID")
    private Long id;

    @Parameter(description = "提及类型：user/topic")
    private String mentionType;

    @Parameter(description = "目标ID")
    private Long targetId;

    @Parameter(description = "目标用户名（user 提及）")
    private String targetUsername;

    @Parameter(description = "目标用户昵称（user 提及）")
    private String targetNickname;

    @Parameter(description = "起始下标（含）")
    private Long startIdx;

    @Parameter(description = "结束下标（不含）")
    private Long endIdx;
}
