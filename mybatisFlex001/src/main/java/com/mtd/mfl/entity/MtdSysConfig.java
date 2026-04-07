package com.mtd.mfl.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.handler.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 系统公共配置表 实体类。
 *
 * @author user
 * @since 2026-04-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("mtd_sys_config")
public class MtdSysConfig extends MyBaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
    @Column(typeHandler = JacksonTypeHandler.class)
    private Object jsonVal;


}
