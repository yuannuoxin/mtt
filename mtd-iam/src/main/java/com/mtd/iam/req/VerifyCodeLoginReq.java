package com.mtd.iam.req;

import lombok.Data;

@Data
public class VerifyCodeLoginReq {
    private Long id;
    private String code;
}
