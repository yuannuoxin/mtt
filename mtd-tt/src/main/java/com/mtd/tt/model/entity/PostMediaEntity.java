package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("post_medias")
@FieldNameConstants
public class PostMediaEntity extends BaseEntity {
    private Long postId;
    private String mediaType;
    /** 指向 user_files.id：原图/封面 */
    private Long originalFileId;
    /** 指向 user_files.id：缩略图 */
    private Long thumbnailFileId;
    /** 指向 user_files.id：视频/音频文件 */
    private Long playFileId;
    private String originalUrl;
    private String thumbnailUrl;
    private String playUrl;
    private Integer width;
    private Integer height;
    private BigDecimal duration;
    private OffsetDateTime createdAt;
}

