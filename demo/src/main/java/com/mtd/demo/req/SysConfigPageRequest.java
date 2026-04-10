package com.mtd.demo.req;

import com.mtd.common.core.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置分页查询请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统配置分页查询请求")
public class SysConfigPageRequest extends PageRequest {

    /**
     * 配置键（模糊查询）
     */
    @Schema(description = "配置键（模糊查询）", example = "max")
    private String configKey;

    /**
     * 配置名称（模糊查询）
     */
    @Schema(description = "配置名称（模糊查询）", example = "上传")
    private String configName;
}
