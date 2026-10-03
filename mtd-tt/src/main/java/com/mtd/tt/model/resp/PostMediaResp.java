package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 帖子媒体响应
 */
@Data
public class PostMediaResp implements Serializable {

    @Parameter(description = "媒体ID")
    private Long id;

    @Parameter(description = "媒体类型：image/video/audio")
    private String mediaType;

    @Parameter(description = "原图地址")
    private String originalUrl;

    @Parameter(description = "缩略图地址")
    private String thumbnailUrl;

    @Parameter(description = "播放地址")
    private String playUrl;

    @Parameter(description = "宽")
    private Integer width;

    @Parameter(description = "高")
    private Integer height;

    @Parameter(description = "时长（秒）")
    private BigDecimal duration;
}
