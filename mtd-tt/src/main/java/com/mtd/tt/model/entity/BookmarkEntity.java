package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("bookmarks")
@FieldNameConstants
public class BookmarkEntity extends BaseEntity {
    private Long userId;
    private Long postId;
    private OffsetDateTime createdAt;
}
