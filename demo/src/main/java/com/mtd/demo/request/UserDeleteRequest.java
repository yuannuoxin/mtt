package com.mtd.demo.request;

import com.mtd.common.core.request.IdRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 删除用户请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDeleteRequest extends IdRequest {
}
