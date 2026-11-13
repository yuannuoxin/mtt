package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.io.Serializable;

/**
 * 下线指定设备请求
 */
@Data
public class KickDeviceReq implements Serializable {

    @Parameter(description = "设备 ID（登录设备列表返回的 deviceId）", required = true)
    private String deviceId;
}
