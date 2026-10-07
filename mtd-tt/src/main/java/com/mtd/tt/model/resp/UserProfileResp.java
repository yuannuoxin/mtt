package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 用户资料响应
 */
@Data
public class UserProfileResp implements Serializable {

    @Parameter(description = "用户ID")
    private Long id;

    @Parameter(description = "用户名")
    private String username;

    @Parameter(description = "昵称")
    private String nickname;

    @Parameter(description = "邮箱")
    private String email;

    @Parameter(description = "头像")
    private String avatar;

    @Parameter(description = "头像压缩图（主页先展示压缩图，点击后再看原图）")
    private String avatarThumb;

    @Parameter(description = "横幅大图")
    private String banner;

    @Parameter(description = "横幅压缩图")
    private String bannerThumb;

    @Parameter(description = "个人简介")
    private String bio;

    @Parameter(description = "状态：active/frozen/banned")
    private String status;

    @Parameter(description = "是否私密账号")
    private Boolean userPrivate;

    @Parameter(description = "注册时间")
    private OffsetDateTime createdAt;

    // ===== 关系与统计（列表/详情时由服务端计算） =====
    @Parameter(description = "当前登录用户是否已关注该用户")
    private Boolean followed;

    @Parameter(description = "当前登录用户是否已拉黑该用户")
    private Boolean blocked;

    @Parameter(description = "关注数")
    private Long followingCount;

    @Parameter(description = "粉丝数")
    private Long followerCount;

    @Parameter(description = "发帖数")
    private Long postCount;

    @Parameter(description = "积分余额")
    private Long points;

    @Parameter(description = "是否管理员")
    private Boolean admin;
}
