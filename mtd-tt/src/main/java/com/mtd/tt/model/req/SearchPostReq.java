package com.mtd.tt.model.req;

import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 搜索帖子请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SearchPostReq extends PageRequest {

    @Parameter(description = "搜索关键词（匹配正文内容）", required = true)
    private String keyword;
}
