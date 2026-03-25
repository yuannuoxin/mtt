package com.mtd.iam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 二次验证方式枚举
 */
@Getter
@AllArgsConstructor
public enum SecondFactorTypeEnum {

    MFA("mfa", "MFA 动态验证码"),
    EMAIL("email", "邮箱验证码"),
    SMS("sms", "短信验证码");

    private final String type;
    private final String desc;

    /**
     * 根据 type 获取枚举
     */
    public static SecondFactorTypeEnum getByType(String type) {
        if (type == null) {
            return null;
        }
        for (SecondFactorTypeEnum factor : values()) {
            if (factor.getType().equals(type)) {
                return factor;
            }
        }
        return null;
    }

    /**
     * 判断是否是有效的验证类型
     */
    public static boolean isValidType(String type) {
        return getByType(type) != null;
    }
}
