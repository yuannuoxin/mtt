package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class IdReq extends BaseRequest {

    @Parameter(description = "ID", required = true)
    private Long id;
}
