package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PollVoteReq extends BaseRequest {
    @Parameter(description = "帖子ID", required = true)
    private Long postId;
    @Parameter(description = "选中的选项下标；多选可传多个", required = true)
    private List<Integer> optionIndexes;
}
