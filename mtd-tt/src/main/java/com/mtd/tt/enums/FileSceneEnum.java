package com.mtd.tt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件上传场景：作为对象存储的第一级目录（avatar/banner/post/...）
 */
@Getter
@AllArgsConstructor
public enum FileSceneEnum {

    AVATAR("avatar", "头像"),
    BANNER("banner", "背景图"),
    POST("post", "推文媒体");

    private final String code;
    private final String desc;

    public static final String OPTIONS = "avatar/banner/post";

    public static FileSceneEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (FileSceneEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
