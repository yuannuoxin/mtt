package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 引用 / 转推列表响应
 */
@Data
public class RelationResp implements Serializable {

    @Parameter(description = "列表")
    private List<PostResp> list;

    @Parameter(description = "总数")
    private Long total;

    @Parameter(description = "是否还有更多")
    private Boolean hasMore;

    @Parameter(description = "当前页码")
    private Integer pageNum;
}
