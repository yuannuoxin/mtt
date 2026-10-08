package com.mtd.tt.model.resp;

import lombok.Data;

import java.io.Serializable;

/** Telegram Login Widget 前端配置：bot username / public key / origin（不泄露 bot token） */
@Data
public class TelegramConfigResp implements Serializable {
    private boolean enabled;
    private String botUsername;
    private String publicKey;
    private String origin;
}
