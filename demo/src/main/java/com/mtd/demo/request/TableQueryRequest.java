package com.mtd.demo.request;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 通用表查询请求参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "通用表查询请求")
public class TableQueryRequest extends PageRequest {
    private String mapper;
    private String tableName;


    // 所有查询条件
    private Map<String, Object> eq;    // 等值查询  deleted=0
    private Map<String, Object> like;  // 模糊查询 username like %xxx%
    private Map<String, Object> gt;   // 大于 id>5
    private Map<String, Object> ge;   // 大于等于
    private Map<String, Object> lt;   // 小于


    // ====================== 多字段排序（数组）
    private List<Map<String, String>> sort; // 格式：[{"field":"create_time","order":"desc"},{"field":"id","order":"asc"}]
}
