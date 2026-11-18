package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 详情页对话视图请求：当前帖 + 上文链 + 直接回复（afterId 为直接回复的升序游标）
 */
@Data
public class ThreadReq implements Serializable {

    @NotNull
    @Parameter(description = "帖子ID")
    private Long postId;

    @Parameter(description = "游标：只取 id 大于该值的直接回复（首页传 0 或不传）")
    private Long afterId;

    @Parameter(description = "每页条数，默认 20")
    private Integer pageSize;
}
