package com.mtd.common.core.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一分页查询请求参数
 * 所有分页查询接口都应继承此类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageRequest extends BaseRequest {

    /**
     * 当前页码（默认第一页）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小（默认 10 条）
     */
    private Integer pageSize = 10;

    /**
     * 排序字段（可选）
     */
    private String sortField;

    /**
     * 排序方式：asc-升序 desc-降序（可选）
     */
    private String sortOrder = "desc";
}
