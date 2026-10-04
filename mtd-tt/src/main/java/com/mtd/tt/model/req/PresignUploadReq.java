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
}
