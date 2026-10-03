package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("notifications")
@FieldNameConstants
public class NotificationEntity extends BaseEntity {
    private Long receiverUserId;
    private Long triggerUserId;
    /**
     * like / comment / follow / repost / mention
     */
    private String type;
    private Long postId;
    private Boolean read;
    private OffsetDateTime createdAt;
}
