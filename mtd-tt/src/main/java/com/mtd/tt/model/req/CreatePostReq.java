package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 创建帖子请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreatePostReq extends BaseRequest {

    @Parameter(description = "帖子类型，可选值：original/reply/quote/repost（见 PostTypeEnum）", required = true)
    private String type;

    @Parameter(description = "正文内容")
    private String content;

    @Parameter(description = "被引用帖子ID（quote/reply/repost 时必填）")
    private Long referencedPostId;

    @Parameter(description = "根帖子ID（reply 时必填，用于话题串）")
    private Long rootPostId;

    @Parameter(description = "可见性，可选值：public/followers_only（见 PostVisibilityEnum），默认 public")
    private String visibility;

    @Parameter(description = "回复权限，可选值：anyone/followed/mentioned（见 ReplyPermissionEnum），默认 anyone")
    private String replyPermission;

    @Parameter(description = "文章标题（传了即按文章发布，仅 original 可用，≤120 字）")
    private String articleTitle;

    @Parameter(description = "文章封面图 URL")
    private String articleCoverUrl;

    @Parameter(description = "媒体列表")
    private List<CreatePostMediaReq> medias;

    @Parameter(description = "@提及列表")
    private List<CreatePostMentionReq> mentions;

    @Parameter(description = "链接卡片")
    private CreatePostLinkCardReq linkCard;

    @Parameter(description = "Turnstile 人机校验 token（后端启用 Turnstile 时必填）")
    private String turnstileToken;
}
