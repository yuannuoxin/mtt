package com.mtd.tt.model.req;

import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 某用户的帖子列表请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPostsReq extends PageRequest {

    @Parameter(description = "目标用户ID", required = true)
    private Long userId;

    @Parameter(description = "帖子类型过滤（original/reply/quote/repost，可不传查全部）")
    private String postType;

    @Parameter(description = "游标：上一页最后一条帖子的 id（首次查询不传，取更早一页）")
    private Long maxId;
}
