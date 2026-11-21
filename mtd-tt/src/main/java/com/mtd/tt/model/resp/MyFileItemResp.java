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
    /** 已绑定的推文 id；非空时不允许删除 */
    private Long postId;
    /** 原图对应的压缩缩略图地址（无缩略图时为空） */
    private String thumbnailUrl;
    /** 原图对应的缩略图文件 id，供选库发帖时绑定 */
    private Long thumbnailFileId;
    private OffsetDateTime createdAt;
}
