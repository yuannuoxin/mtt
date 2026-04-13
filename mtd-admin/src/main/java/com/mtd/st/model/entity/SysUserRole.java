package com.mtd.st.model.entity;

import com.mtd.common.mybatis.base.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_role")
@FieldNameConstants
public class SysUserRole extends BaseEntity {

    private Long userId;
    private Long roleId;
}
