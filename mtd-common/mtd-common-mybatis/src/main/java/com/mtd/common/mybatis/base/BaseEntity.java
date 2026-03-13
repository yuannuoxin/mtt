package com.mtd.common.mybatis.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类，包含公共字段
 */
@Data
@FieldNameConstants
public abstract class BaseEntity implements Serializable {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间 - Java 规范字段
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间 - Java 规范字段
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人 ID - Java 规范字段
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人 ID - Java 规范字段
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 逻辑删除标识
     */
    @TableLogic
    private Integer deleted;
}
