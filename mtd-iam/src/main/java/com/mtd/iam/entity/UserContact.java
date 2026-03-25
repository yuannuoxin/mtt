package com.mtd.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mtd.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iam_user_contact")
@FieldNameConstants
public class UserContact extends BaseEntity {

    private Long userId;
    /**
     * 联系类型：sms/email
     */
    private String contactType;
    /**
     * 联系方式
     */
    private String contact;
    /**
     * 国家区号 如 +86 / +1 / +44 / +852
     */
    private String countryCode;
    /**
     * 是否主账号：1-主账号 0-辅助
     */
    private Integer major;
    /**
     * 是否验证：0-未验证 1-已验证
     */
    private Integer verified;
    /**
     * 状态
     */
    private Integer status;
}
