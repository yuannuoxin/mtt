package com.mtd.common.core.exception;

import com.mtd.common.core.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常基类（不需要关注的异常）
 * 用于正常的业务逻辑校验，记录 INFO 级别日志
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }
}
