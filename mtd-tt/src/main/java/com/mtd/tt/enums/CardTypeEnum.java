package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 链接卡片类型
 */
@Getter
@AllArgsConstructor
public enum CardTypeEnum {

    LINK("link", "普通链接卡片"),
    EMBED_VIDEO("embed_video", "内嵌视频卡片");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "link/embed_video";

    public static CardTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (CardTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
