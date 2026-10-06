package com.mtd.tt.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改个人资料请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateProfileReq extends BaseRequest {

    @Parameter(description = "昵称")
    private String nickname;

    @Parameter(description = "头像地址")
    private String avatar;

    @Parameter(description = "横幅大图")
    private String banner;

    @Parameter(description = "个人简介")
    private String bio;

    @Parameter(description = "是否私密账号（true=私密）")
    private Boolean userPrivate;

    @Parameter(description = "新邮箱（需配合 emailCode 验证码）")
    private String email;

    @Parameter(description = "新邮箱验证码（修改邮箱时必填）")
    private String emailCode;

    @Parameter(description = "当前密码（修改密码时必填）")
    private String oldPassword;

    @Parameter(description = "新密码（修改密码时填写，6-32 位）")
    private String newPassword;

    @Parameter(description = "Turnstile 人机校验 token（后端启用 Turnstile 时必填）")
    private String turnstileToken;
}
