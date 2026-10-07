package com.mtd.tt.model.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 签到结果
 */
@Data
public class CheckinResp implements Serializable {
    private String type;
    private Integer reward;
    private Long balance;
    private Boolean checkedIn;
}
