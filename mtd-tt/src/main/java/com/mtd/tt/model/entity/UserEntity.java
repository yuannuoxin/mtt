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
     * 头像压缩图（主页先展示压缩图，点击后再看原图）
     */
    private String avatarThumb;
    /**
     * 横幅大图
     */
    private String banner;
    /**
     * 横幅压缩图
     */
    private String bannerThumb;
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
    /**
     *  user_private: 是否私密账号
     */
    private Boolean userPrivate;
    /**
     * 积分余额
     */
    private Long points;

    /**
     *  is_admin: 是否管理员（默认 false，仅管理员可修改系统配置）
     */
    private Boolean isAdmin;
}
