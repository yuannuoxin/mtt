package com.mtd.demo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统配置保存请求参数
 */
@Data
@Schema(description = "系统配置保存请求")
public class SysConfigSaveRequest {

    /**
     * 主键ID（更新时必填）
     */
    @Schema(description = "主键ID", example = "1")
    private Long id;

    /**
     * 配置唯一键
     */
    @NotBlank(message = "配置键不能为空")
    @Schema(description = "配置唯一键", example = "max_upload", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configKey;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空")
    @Schema(description = "配置名称", example = "最大上传大小", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configName;

    /**
     * 配置值（任意对象，会自动序列化为JSON）
     */
    @Schema(description = "配置值（任意对象）", example = "{\"size\": 10485760, \"unit\": \"bytes\"}")
    private Object jsonVal;
}
