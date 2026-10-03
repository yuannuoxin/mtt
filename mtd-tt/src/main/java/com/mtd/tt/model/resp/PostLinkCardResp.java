package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;

/**
 * 帖子链接卡片响应
 */
@Data
public class PostLinkCardResp implements Serializable {

    @Parameter(description = "卡片ID")
    private Long id;

    @Parameter(description = "卡片类型：link/embed_video")
    private String cardType;

    @Parameter(description = "目标链接")
    private String targetUrl;

    @Parameter(description = "视频平台：bilibili/youtube")
    private String videoProvider;

    @Parameter(description = "第三方视频ID")
    private String videoId;

    @Parameter(description = "封面图地址")
    private String coverMediaUrl;
}
