package com.mtd.tt.model.req;

import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 首页时间线请求（推荐 / 关注）。游标分页：maxId 为上一页最后一条帖子的 id，取更早的下一页。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HomeTimelineReq extends PageRequest {

    @Parameter(description = "游标：上一页最后一条帖子的 id（首次查询不传，取更早一页）")
    private Long maxId;
}
