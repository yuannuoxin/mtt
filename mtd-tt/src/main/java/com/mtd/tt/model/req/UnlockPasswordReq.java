package com.mtd.tt.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 密码解锁隐藏内容请求。
 */
@Data
public class UnlockPasswordReq {

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "作者设置的查看密码")
    private String password;
}
