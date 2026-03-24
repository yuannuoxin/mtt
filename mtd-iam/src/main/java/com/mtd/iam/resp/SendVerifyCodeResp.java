package com.mtd.iam.resp;

import lombok.Data;

@Data
public class SendVerifyCodeResp {
    private Long id;
    private String code;
}
