package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

/**
 * 经验变更记录：发帖/转发/引用/回复等每次经验变动落一条。
 * points 正数=获得；balance 为变动后的累计经验。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("exp_records")
@FieldNameConstants
public class ExpRecordEntity extends BaseEntity {
    private Long userId;
    /** 类型：post/quote/repost/reply */
    private String type;
    private Long points;
    private Long balance;
    private String remark;
    private OffsetDateTime createdAt;
}
