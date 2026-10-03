package com.mtd.tt.model.entity;


import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Table("users")
@FieldNameConstants
public class UserEntity {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String banner;
    private String bio;
    private Integer status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
