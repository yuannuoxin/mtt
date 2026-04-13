package com.mtd.st.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageReq extends PageReq {

    @Parameter(description = "用户名")
    private String username;

    @Parameter(description = "昵称")
    private String nickname;

    @Parameter(description = "邮箱")
    private String email;

    @Parameter(description = "手机号")
    private String phone;

    @Parameter(description = "状态: 0禁用 1正常")
    private Integer status;
}
