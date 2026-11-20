package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 当前用户上传过的媒体文件（文章编辑器「我的图片 / 我的视频」）
 */
@Data
@Schema(description = "用户媒体文件")
public class MyFileItemResp {
    private Long id;
    private String url;
    private String mediaType;
    private String contentType;
    private Long size;
    private OffsetDateTime createdAt;
}
