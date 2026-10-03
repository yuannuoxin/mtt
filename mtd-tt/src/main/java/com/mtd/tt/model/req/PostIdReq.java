package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子 ID 请求（详情/点赞/收藏/删除等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostIdReq extends BaseRequest {

    @Parameter(description = "帖子ID", required = true)
    private Long postId;
}
