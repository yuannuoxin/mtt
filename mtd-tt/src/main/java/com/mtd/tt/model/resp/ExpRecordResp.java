package com.mtd.tt.model.resp;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 经验变更记录
 */
@Data
public class ExpRecordResp implements Serializable {
    private Long id;
    private String type;
    private String typeDesc;
    private Long points;
    private Long balance;
    private String remark;
    private OffsetDateTime createdAt;
}
