package com.mtd.tt.model.entity;


import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("user_timeline")
@FieldNameConstants
public class UserTimelineEntity extends BaseEntity {
    private Long userId;
    private String itemType;
    private Long itemId;
    private Long itemUserId;
    private Boolean valid;

    private OffsetDateTime createdAt;
}
