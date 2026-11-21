package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预签名上传请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PresignUploadReq extends BaseRequest {

    @Parameter(description = "文件名（用于推断扩展名）")
    private String fileName;

    @Parameter(description = "文件 Content-Type，如 image/png")
    private String contentType;

    @Parameter(description = "上传场景 FileSceneEnum：avatar/banner/post，默认 post")
    private String scene;

    @Parameter(description = "是否缩略图（路径体现 _thumb）")
    private Boolean thumbnail;

    @Parameter(description = "缩略图所属原图的 user_files.id（仅 thumbnail=true 时有效）")
    private Long thumbnailOfFileId;

    @Parameter(description = "文件大小（字节），用于按上传大小扣积分（不足1MB按1MB）")
    private Long size;
}
