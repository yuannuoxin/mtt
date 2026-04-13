package com.mtd.st.model.resp;

import cn.dev33.satoken.stp.SaTokenInfo;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.util.List;

@Data
public class LoginResp {

    @Parameter(description = "Token信息")
    private SaTokenInfo tokenInfo;

    @Parameter(description = "用户ID")
    private Long userId;

    @Parameter(description = "用户名")
    private String username;

    @Parameter(description = "昵称")
    private String nickname;
}
