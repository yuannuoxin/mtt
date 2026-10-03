package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 媒体类型
 */
@Getter
@AllArgsConstructor
public enum MediaTypeEnum {

    IMAGE("image", "图片"),
    VIDEO("video", "视频"),
    AUDIO("audio", "音频");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "image/video/audio";

    public static MediaTypeEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (MediaTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
