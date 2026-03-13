package com.mtd.common.core.exception;

import com.mtd.common.core.result.ResultCode;
import lombok.Getter;

/**
 * 关键业务异常（需要关注的异常）
 * 用于重要的业务场景失败，记录 ERROR 级别日志和完整堆栈
 * 例如：支付失败、第三方服务调用失败、数据库操作失败等
 */
@Getter
public class CriticalBusinessException extends RuntimeException {
    
    private final Integer code;
    private final String message;

    public CriticalBusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
        this.message = message;
    }

    public CriticalBusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public CriticalBusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }
}
