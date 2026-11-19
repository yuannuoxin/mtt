package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 详情页对话视图:当前帖 + 上文链(root → parent)+ 整条对话的平铺回复
 */
@Data
public class ThreadResp implements Serializable {

    @Parameter(description = "当前帖")
    private PostResp post;

    @Parameter(description = "上文链，按 root → parent 顺序；当前帖不是回复时为空")
    private List<PostResp> ancestors;

    @Parameter(description = "整条对话的回复，平铺（不含当前帖与上方祖先）")
    private List<PostResp> replies;

    @Parameter(description = "该对话回复总数")
    private Long total;

    @Parameter(description = "是否还有更多回复")
    private Boolean hasMore;

    @Parameter(description = "当前页码")
    private Integer pageNum;

    @Parameter(description = "被引用次数")
    private Long quoteCount;

    @Parameter(description = "被转推次数")
    private Long repostCount;
}
