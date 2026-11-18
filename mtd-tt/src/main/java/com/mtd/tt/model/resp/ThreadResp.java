package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 详情页对话视图：当前帖 + 上文链（root → parent）+ 直接回复
 */
@Data
public class ThreadResp implements Serializable {

    @Parameter(description = "当前帖")
    private PostResp post;

    @Parameter(description = "上文链，按 root → parent 顺序；当前帖不是回复时为空")
    private List<PostResp> ancestors;

    @Parameter(description = "整条对话的回复，平铺、按 id 升序（不含当前帖与上方祖先）")
    private List<PostResp> replies;

    @Parameter(description = "是否还有更多直接回复")
    private Boolean hasMore;

    @Parameter(description = "下一页游标（本次最后一条回复的 id）")
    private Long nextCursor;
}
