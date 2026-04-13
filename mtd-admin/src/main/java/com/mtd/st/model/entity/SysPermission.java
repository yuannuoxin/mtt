package com.mtd.st.model.entity;

import com.mtd.common.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_permission")
@FieldNameConstants
public class SysPermission extends BaseEntity {

    private Long parentId;
    private String permCode;
    private String permName;
    private Integer permType;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer status;
}
