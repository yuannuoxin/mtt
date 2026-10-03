package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 通知响应
 */
@Data
public class NotificationResp implements Serializable {

    @Parameter(description = "通知ID")
    private Long id;

    @Parameter(description = "通知类型：like/comment/follow/repost/mention")
    private String type;

    @Parameter(description = "触发用户ID")
    private Long triggerUserId;

    @Parameter(description = "触发用户信息")
    private UserProfileResp triggerUser;

    @Parameter(description = "关联帖子ID")
    private Long postId;

    @Parameter(description = "是否已读")
    private Boolean read;

    @Parameter(description = "通知时间")
    private OffsetDateTime createdAt;
}
