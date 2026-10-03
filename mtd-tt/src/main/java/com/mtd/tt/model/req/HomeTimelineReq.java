package com.mtd.tt.model.req;

import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 首页关注时间线请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HomeTimelineReq extends PageRequest {

}
