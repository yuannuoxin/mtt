package com.mtd.iam.resp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * MFA 信息响应
 */
@Data
public class MfaInfoResp {

    @Parameter(description = "MFA 记录 ID")
    private Long id;

    @Parameter(description = "用户 ID")
    private Long userId;

    @Parameter(description = "是否已启用 MFA")
    private Boolean enabled;

    @Parameter(description = "密钥（仅在添加时返回）")
    private String secret;

    @Parameter(description = "二维码 URL（用于身份验证器扫描）")
    private String qrCodeUrl;
}
