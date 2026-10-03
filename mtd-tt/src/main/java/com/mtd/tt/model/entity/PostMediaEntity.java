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
    private String originalUrl;
    private String thumbnailUrl;
    private String playUrl;
    private Integer width;
    private Integer height;
    private BigDecimal duration;
    private OffsetDateTime createdAt;
}

