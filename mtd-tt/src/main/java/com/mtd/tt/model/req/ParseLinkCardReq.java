package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 链接卡片解析请求（预览用，不落库）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParseLinkCardReq extends BaseRequest {

    @Parameter(description = "待解析的链接", required = true)
    private String url;
}
