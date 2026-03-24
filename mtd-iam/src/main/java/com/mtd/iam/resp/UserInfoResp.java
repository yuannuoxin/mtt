package com.mtd.iam.resp;

import lombok.Data;

/**
 * 用户信息响应
 */
@Data
public class UserInfoResp {
    private Long id;
    private String username;
    private String nickname;
    private Integer status;
}
