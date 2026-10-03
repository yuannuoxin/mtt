package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

/**
 * 实体基类，包含公共字段
 */
@Data
@FieldNameConstants
public abstract class BaseEntity implements Serializable {
    /**
     * 主键 ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;
}
