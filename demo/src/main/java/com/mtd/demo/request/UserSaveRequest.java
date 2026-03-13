package com.mtd.demo.request;

import com.mtd.common.core.request.BaseRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保存/更新用户请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSaveRequest extends BaseRequest {

    @Parameter(description = "用户 ID（更新时必填）")
    private Long id;
    
    @Parameter(description = "用户名", required = true)
    private String username;
    
    @Parameter(description = "密码（创建时必填）")
    private String password;
    
    @Parameter(description = "昵称")
    private String nickname;
    
    @Parameter(description = "邮箱")
    private String email;
    
    @Parameter(description = "手机号")
    private String phone;
    
    @Parameter(description = "状态：0-禁用 1-正常")
    private Integer status;
}
