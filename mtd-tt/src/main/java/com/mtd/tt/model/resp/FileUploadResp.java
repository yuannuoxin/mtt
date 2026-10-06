package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传响应
 */
@Data
public class FileUploadResp implements Serializable {

    @Parameter(description = "文件访问地址（后端直传），或预签名 PUT 直传地址（预签名直传）")
    private String url;

    @Parameter(description = "文件记录 id（user_files.id），发推时用于关联")
    private Long fileId;

    @Parameter(description = "上传场景 FileSceneEnum")
    private String scene;
}
