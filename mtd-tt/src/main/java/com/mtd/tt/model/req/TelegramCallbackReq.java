package com.mtd.tt.model.req;

import lombok.Data;

/**
 * Telegram Login Widget 回调：widget 返回的已签名用户数据（含 hash），后端用 bot token 验签。
 */
@Data
public class TelegramCallbackReq {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String photoUrl;
    private Long authDate;
    private String hash;
}
