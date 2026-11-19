package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 引用 / 转推列表请求
 */
@Data
public class RelationReq implements Serializable {

    @NotNull
    @Parameter(description = "帖子ID")
    private Long postId;

    @Parameter(description = "类型：quote（引用）| repost（转推）")
    private String type;

    @Parameter(description = "排序：latest（最新，默认）| liked（最多点赞）")
    private String sort;

    @Parameter(description = "页码，从 1 开始")
    private Integer pageNum;

    @Parameter(description = "每页条数，默认 20")
    private Integer pageSize;
}
