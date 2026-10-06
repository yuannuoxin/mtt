package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建帖子-媒体项
 */
@Data
public class CreatePostMediaReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Parameter(description = "媒体类型，可选值：image/video/audio（见 MediaTypeEnum）")
    private String mediaType;
    @Parameter(description = "原图文件id（user_files.id，客户端上传后带回，替代按url匹配）")
    private Long originalFileId;
    @Parameter(description = "缩略图文件id（user_files.id）")
    private Long thumbnailFileId;
    @Parameter(description = "播放文件id（user_files.id，视频/音频）")
    private Long playFileId;
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
