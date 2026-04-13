package com.mtd.st.model.req;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserCreateReq extends BaseRequest {

    @NotBlank(message = "用户名不能为空")
    @Parameter(description = "用户名", required = true)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Parameter(description = "密码", required = true)
    private String password;

    @Parameter(description = "昵称")
    private String nickname;

    @Parameter(description = "邮箱")
    private String email;

    @Parameter(description = "手机号")
    private String phone;

    @Parameter(description = "头像")
    private String avatar;

    @Parameter(description = "状态: 0禁用 1正常")
    private Integer status;
}
