package com.mtd.iam.resp;

import cn.dev33.satoken.stp.SaTokenInfo;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

import java.util.List;

/**
 * 登录响应（支持二次验证）
 */
@Data
public class LoginResp {

    @Parameter(description = "是否需要二次验证")
    private Boolean needSecondFactor;

    @Parameter(description = "Token 信息（不需要二次验证时返回）")
    private SaTokenInfo tokenInfo;

    @Parameter(description = "二次验证 ID（需要二次验证时返回）")
    private String secondFactorId;

    @Parameter(description = "二次验证类型：email/sms/mfa（需要二次验证时返回）")
    private String secondFactorType;

    @Parameter(description = "二次验证方式描述（需要二次验证时返回）")
    private String secondFactorDesc;

    @Parameter(description = "支持的二次验证类型列表（需要二次验证时返回）")
    private List<String> supportedFactorTypes;

    @Parameter(description = "可用的联系方式列表（选择 email/sms 时返回）")
    private List<String> contactList;

    /**
     * 创建登录成功的响应
     */
    public static LoginResp createLoginSuccess(SaTokenInfo tokenInfo) {
        LoginResp resp = new LoginResp();
        resp.setNeedSecondFactor(false);
        resp.setTokenInfo(tokenInfo);
        return resp;
    }
}
