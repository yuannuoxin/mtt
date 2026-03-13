package com.mtd.common.core.result;

import lombok.Getter;

/**
 * 统一响应状态码枚举
 */
@Getter
public enum ResultCode {
    
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    
    TOKEN_INVALID(1001, "Token 无效"),
    TOKEN_EXPIRED(1002, "Token 已过期"),
    LOGIN_ERROR(1003, "登录失败"),
    PERMISSION_DENIED(1004, "权限不足");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
