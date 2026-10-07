package com.mtd.tt.model.resp;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 积分变更记录
 */
@Data
public class PointRecordResp implements Serializable {
    private Long id;
    private String type;
    private String typeDesc;
    private Long points;
    private Long balance;
    private String remark;
    private OffsetDateTime createdAt;
}
