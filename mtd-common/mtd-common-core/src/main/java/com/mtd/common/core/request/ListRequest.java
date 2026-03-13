package com.mtd.common.core.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一列表查询请求参数（不分页）
 * 所有列表查询接口都应继承此类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ListRequest extends BaseRequest {

    /**
     * 排序字段（可选）
     */
    private String sortField;

    /**
     * 排序方式：asc-升序 desc-降序（可选）
     */
    private String sortOrder = "desc";
}
