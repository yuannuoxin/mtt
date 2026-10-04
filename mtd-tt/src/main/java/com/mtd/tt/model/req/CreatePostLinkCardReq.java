package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建帖子-链接卡片
 */
@Data
public class CreatePostLinkCardReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Parameter(description = "卡片类型，可选值：link/embed_video（见 CardTypeEnum）")
    private String cardType;
    @Parameter(description = "目标链接")
    private String targetUrl;
    @Parameter(description = "页面标题")
    private String title;
    @Parameter(description = "页面描述")
    private String description;
    @Parameter(description = "视频平台，可选值：bilibili/youtube（见 VideoProviderEnum）")
    private String videoProvider;
    @Parameter(description = "第三方视频ID")
    private String videoId;
    @Parameter(description = "封面图地址")
    private String coverMediaUrl;
}
