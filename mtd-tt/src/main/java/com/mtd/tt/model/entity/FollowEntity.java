package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("follows")
@FieldNameConstants
public class FollowEntity extends BaseEntity {
    private Long followerId;
    /**
     * 被关注者
     */
    private Long followeeId;
    private OffsetDateTime createdAt;
}
