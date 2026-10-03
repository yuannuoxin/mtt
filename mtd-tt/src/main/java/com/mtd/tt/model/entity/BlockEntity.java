package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("blocks")
@FieldNameConstants
public class BlockEntity extends BaseEntity {
    private Long userId;
    private Long blockedUserId;
    private OffsetDateTime createdAt;
}
