package com.mtd.tt.model.req;

import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章列表请求（发现 / 我的作品）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleListReq extends PageRequest {

    @Parameter(description = "范围：all=全站发现（默认） / mine=我的作品")
    private String scope;
}
