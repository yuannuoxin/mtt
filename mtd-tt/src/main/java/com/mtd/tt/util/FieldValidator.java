package com.mtd.tt.util;

import cn.hutool.core.util.StrUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;

import java.util.regex.Pattern;

/**
 * 用户字段校验工具：用户名 / 昵称 / 邮箱 / 密码。
 * <p>注册、编辑个人资料、忘记密码等场景复用同一套规则，避免各处校验不一致。</p>
 */
public final class FieldValidator {

    /** 用户名：3~20 位，字母开头，仅字母/数字/下划线 */
    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 20;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{2,19}$");

    /** 昵称：1~30 个字符 */
    public static final int NICKNAME_MIN = 1;
    public static final int NICKNAME_MAX = 30;

    /** 密码：6~32 位 */
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 32;

    /** 邮箱标准格式 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private FieldValidator() {
    }

    public static void validateUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名不能为空");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "用户名须为 " + USERNAME_MIN + "-" + USERNAME_MAX + " 位，字母开头，仅字母/数字/下划线");
        }
    }

    public static void validateNickname(String nickname) {
        if (StrUtil.isBlank(nickname)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "昵称不能为空");
        }
        if (nickname.length() < NICKNAME_MIN || nickname.length() > NICKNAME_MAX) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "昵称长度须为 " + NICKNAME_MIN + "-" + NICKNAME_MAX + " 个字符");
        }
    }

    public static void validateEmail(String email) {
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "邮箱不能为空");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "邮箱格式不正确");
        }
    }

    public static void validatePassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "密码不能为空");
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "密码长度须为 " + PASSWORD_MIN + "-" + PASSWORD_MAX + " 位");
        }
    }
}
