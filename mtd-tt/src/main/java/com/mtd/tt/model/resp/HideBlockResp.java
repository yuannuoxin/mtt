package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;

/**
 * 正文隐藏块对当前读者的访问状态。
 * index 与正文中隐藏块顺序一一对应，正文本身按服务端脱敏结果下发。
 */
@Data
public class HideBlockResp implements Serializable {

    @Parameter(description = "隐藏块下标")
    private Integer index;

    @Parameter(description = "可见条件：login/reply/points/password")
    private String kind;

    @Parameter(description = "积分条件所需积分")
    private Integer points;

    @Parameter(description = "当前读者是否已解锁")
    private Boolean unlocked;
}
