package com.mtd.tt.model.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 登录设备（一个 token 会话即一台设备）
 */
@Data
public class DeviceResp implements Serializable {

    @Parameter(description = "设备 ID（token 哈希，前端用于下线，不暴露真实 token）")
    private String deviceId;

    @Parameter(description = "设备名，如 Chrome · Windows")
    private String name;

    @Parameter(description = "浏览器")
    private String browser;

    @Parameter(description = "操作系统")
    private String os;

    @Parameter(description = "登录 IP")
    private String ip;

    @Parameter(description = "登录方式 code")
    private String provider;

    @Parameter(description = "登录方式名称")
    private String providerDesc;

    @Parameter(description = "登录时的浏览器指纹")
    private String fingerprint;

    @Parameter(description = "登录时间")
    private OffsetDateTime loginTime;

    @Parameter(description = "是否当前正在使用的设备")
    private Boolean current;
}
