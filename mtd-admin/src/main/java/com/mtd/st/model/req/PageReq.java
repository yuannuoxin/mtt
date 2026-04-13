package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PageReq extends BaseRequest {

    @Parameter(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Parameter(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
