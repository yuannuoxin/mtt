package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("posts")
@FieldNameConstants
public class PostEntity extends BaseEntity {
    private Long userId;
    /**
     * original / repost / quote / reply
     */
    private String type;
    private String content;

    private Long referencedPostId;
    private Long rootPostId;
    private String visibility;
    private Integer articleFlag;
    /** 文章标题（articleFlag=1 时有值） */
    private String articleTitle;
    /** 文章封面图 */
    private String articleCoverUrl;

    private Boolean hasMedia;
    private Boolean hasImage;
    private Boolean hasVideo;

    private String replyPermission;

    private Long likeCount;
    private Long repostCount;
    private Long replyCount;
    private Long viewCount;
    private Long bookmarkCount;

    private Long hotScore;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
