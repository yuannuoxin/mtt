package com.mtd.tt.model.req;

import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 某帖子的回复列表请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostRepliesReq extends PageRequest {

    @Parameter(description = "目标帖子ID", required = true)
    private Long postId;

    @Parameter(description = "游标：上一页最后一条回复的 id（首次查询不传，取更早一页）")
    private Long maxId;
}
