package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AcceptAnswerReq extends BaseRequest {
    @Parameter(description = "提问/悬赏帖ID", required = true)
    private Long postId;
    @Parameter(description = "被采纳的回复ID", required = true)
    private Long replyId;
}
