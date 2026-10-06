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

    @Parameter(description = "头像压缩图地址")
    private String avatarThumb;

    @Parameter(description = "横幅大图")
    private String banner;

    @Parameter(description = "横幅压缩图地址")
    private String bannerThumb;

    @Parameter(description = "个人简介")
    private String bio;

    @Parameter(description = "是否私密账号（true=私密）")
    private Boolean userPrivate;

    @Parameter(description = "Turnstile 人机校验 token（后端启用 Turnstile 时必填）")
    private String turnstileToken;
}
