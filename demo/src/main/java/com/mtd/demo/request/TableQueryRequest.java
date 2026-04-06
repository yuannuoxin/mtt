package com.mtd.demo.request;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用表查询请求参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "通用表查询请求")
public class TableQueryRequest extends PageRequest {
    private String tableName;
    private QueryWrapper<?> query;
}
