package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 帖子响应
 */
@Data
public class PostResp implements Serializable {

    @Parameter(description = "帖子ID")
    private Long id;

    @Parameter(description = "作者ID")
    private Long userId;

    @Parameter(description = "作者信息")
    private UserProfileResp user;

    @Parameter(description = "帖子类型：original/reply/quote/repost")
    private String type;

    @Parameter(description = "正文内容")
    private String content;

    @Parameter(description = "被引用帖子ID")
    private Long referencedPostId;

    @Parameter(description = "被引用/被回复的原文帖子（reply/quote/repost 时返回，一层）")
    private PostResp referencedPost;

    @Parameter(description = "根帖子ID")
    private Long rootPostId;

    @Parameter(description = "可见性：public/followers_only")
    private String visibility;

    @Parameter(description = "是否文章")
    private Integer articleFlag;

    @Parameter(description = "是否有媒体")
    private Boolean hasMedia;

    @Parameter(description = "是否有图片")
    private Boolean hasImage;

    @Parameter(description = "是否有视频")
    private Boolean hasVideo;

    @Parameter(description = "回复权限")
    private String replyPermission;

    @Parameter(description = "点赞数")
    private Long likeCount;

    @Parameter(description = "回复数")
    private Long replyCount;

    @Parameter(description = "转发数")
    private Long repostCount;

    @Parameter(description = "浏览数")
    private Long viewCount;

    @Parameter(description = "收藏数")
    private Long bookmarkCount;

    @Parameter(description = "热度分")
    private Long hotScore;

    @Parameter(description = "创建时间")
    private OffsetDateTime createdAt;

    @Parameter(description = "媒体列表")
    private List<PostMediaResp> medias;

    @Parameter(description = "@提及列表")
    private List<PostMentionResp> mentions;

    @Parameter(description = "链接卡片")
    private PostLinkCardResp linkCard;

    // ===== 当前登录用户视角 =====
    @Parameter(description = "当前登录用户是否已点赞")
    private Boolean liked;

    @Parameter(description = "当前登录用户是否已收藏")
    private Boolean bookmarked;

    @Parameter(description = "当前登录用户是否已转发")
    private Boolean reposted;

    @Parameter(description = "是否是当前登录用户发布的")
    private Boolean mine;
}
