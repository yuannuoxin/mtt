package com.mtd.demo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * 系统公共配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "mtd_sys_config", autoResultMap = true)
@FieldNameConstants
public class SysConfig extends BaseEntity {

    /**
     * 配置唯一键（如 max_upload）
     */
    private String configKey;

    /**
     * 配置名称（如 最大上传大小）
     */
    private String configName;

    /**
     * 配置值（支持数字、字符串、复杂JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object jsonVal;
}
