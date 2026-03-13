package com.mtd.common.core.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一 ID 查询请求参数
 * 适用于所有只需要一个 ID 参数的接口
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdRequest extends BaseRequest {

    private Long id;
}
