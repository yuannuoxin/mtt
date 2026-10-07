package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

/**
 * 积分变更记录：签到/发帖/回复/上传等每次积分变动都落一条。
 * points 正数=获得，负数=消耗；balance 为变动后的账户余额。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("point_records")
@FieldNameConstants
public class PointRecordEntity extends BaseEntity {
    private Long userId;
    /**
     * 变更类型：checkin/post/quote/repost/reply/upload/register
     */
    private String type;
    private Long points;
    private Long balance;
    private String remark;
    private OffsetDateTime createdAt;
}
