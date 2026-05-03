package com.mtd.common.mybatis.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类，包含公共字段
 */
@Data
@FieldNameConstants
public abstract class BaseEntity2 implements Serializable {

    /**
     * 主键 ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 创建时间 - Java 规范字段
     */
    private LocalDateTime createTime;

    /**
     * 更新时间 - Java 规范字段
     */
    private LocalDateTime updateTime;

    /**
     * 创建人 ID - Java 规范字段
     */
    private Long createBy;

    /**
     * 更新人 ID - Java 规范字段
     */
    private Long updateBy;
}
