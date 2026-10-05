package com.mtd.tt.model.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

/**
 * 系统控制配置表：用于开关 Turnstile 人机校验、邮箱验证码等能力，
 * 运行时通过控制台/接口修改即可，无需改代码重启。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_config")
@FieldNameConstants
public class SysConfigEntity extends BaseEntity {
    private String cfgKey;
    private String cfgValue;
    private String remark;
    private OffsetDateTime updatedAt;
}
