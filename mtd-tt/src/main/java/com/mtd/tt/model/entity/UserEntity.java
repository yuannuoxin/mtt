package com.mtd.tt.model.entity;


import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("users")
@FieldNameConstants
public class UserEntity extends BaseEntity {
    private String username;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatar;
    /**
     * 横幅大图
     */
    private String banner;
    /**
     * 个人简介 / 个人签名
     */
    private String bio;
    /**
     *  users.status(active:正常用户 frozen:临时冻结 banned:永久封禁)
     */
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
    private Integer userPrivate ;
}
