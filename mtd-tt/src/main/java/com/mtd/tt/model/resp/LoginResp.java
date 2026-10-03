package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应
 */
@Data
public class LoginResp implements Serializable {

    @Parameter(description = "token 名称")
    private String tokenName;

    @Parameter(description = "token 值")
    private String tokenValue;

    @Parameter(description = "登录用户信息")
    private UserProfileResp user;
}
