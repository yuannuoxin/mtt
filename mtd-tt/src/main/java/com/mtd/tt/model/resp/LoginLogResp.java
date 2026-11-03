package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 登录记录响应
 */
@Data
public class LoginLogResp implements Serializable {

    @Parameter(description = "记录 ID")
    private Long id;

    @Parameter(description = "登录方式 code：password/email_code/google/github/telegram/discord")
    private String provider;

    @Parameter(description = "登录方式名称")
    private String providerDesc;

    @Parameter(description = "登录时的浏览器指纹")
    private String fingerprint;

    @Parameter(description = "登录 IP")
    private String ip;

    @Parameter(description = "登录 User-Agent")
    private String userAgent;

    @Parameter(description = "登录时间")
    private OffsetDateTime createdAt;
}
