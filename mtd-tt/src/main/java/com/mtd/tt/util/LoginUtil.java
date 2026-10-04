package com.mtd.tt.util;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 可选登录工具：接口默认不强制登录，
 * 已登录返回当前用户 ID，未登录返回 null（按游客处理）。
 */
public final class LoginUtil {

    private LoginUtil() {
    }

    /** 当前登录用户 ID；未登录返回 null */
    public static Long currentUserId() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
    }
}
