package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("post_mentions")
@FieldNameConstants
public class PostMentionEntity extends BaseEntity {
    private Long postId;
    /**
     * 枚举：user @用户 | topic 话题
     */
    private String mentionType;
    private Long targetId;
    private Long startIdx;
    private Long endIdx;
    private OffsetDateTime createdAt;
}
