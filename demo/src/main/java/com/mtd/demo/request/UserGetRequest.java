package com.mtd.demo.request;

import com.mtd.common.core.request.IdRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询用户详情请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserGetRequest extends IdRequest {
}
