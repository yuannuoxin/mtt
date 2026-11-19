package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 详情页对话视图请求：当前帖 + 上文链 + 整条对话的平铺回复
 */
@Data
public class ThreadReq implements Serializable {

    @NotNull
    @Parameter(description = "帖子ID")
    private Long postId;

    @Parameter(description = "回复排序：relevant（最相关，默认）| latest（最新）| liked（最多点赞）")
    private String sort;

    @Parameter(description = "页码，从 1 开始")
    private Integer pageNum;

    @Parameter(description = "每页条数，默认 20")
    private Integer pageSize;
}
