package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内嵌视频平台
 */
@Getter
@AllArgsConstructor
public enum VideoProviderEnum {

    BILIBILI("bilibili", "哔哩哔哩"),
    YOUTUBE("youtube", "YouTube");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "bilibili/youtube";

    public static VideoProviderEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (VideoProviderEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
