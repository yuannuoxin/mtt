package com.mtd.common.web.exception;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.exception.CriticalBusinessException;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 所有响应 HTTP 状态码均为 200，通过业务状态码区分成功/失败
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 - 不需要关注的异常（预期内的异常）
     * 例如：参数校验失败、权限不足、资源不存在等
     * 记录 INFO 级别日志
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.info("业务异常 [{}]: {}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 关键业务异常 - 需要关注的异常（重要业务失败）
     * 例如：支付失败、第三方服务调用失败、数据库操作失败等
     * 记录 ERROR 级别日志和完整堆栈
     */
    @ExceptionHandler(CriticalBusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleCriticalBusinessException(CriticalBusinessException e) {
        log.error("关键业务异常 [{}]: {}", e.getCode(), e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数验证异常 - 不需要在意的异常
     * 例如：请求参数格式错误、缺少必填字段等
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.info("参数验证异常：{}", message);
        return Result.error(ResultCode.PARAM_ERROR);
    }

    /**
     * 参数绑定异常 - 不需要在意的异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(ResultCode.PARAM_ERROR.getMessage());
        log.info("参数绑定异常：{}", message);
        return Result.error(ResultCode.PARAM_ERROR);
    }

    /**
     * 系统异常 - 需要在意的异常（非预期的异常）
     * 例如：空指针、数据库连接失败、第三方服务调用失败等
     * 需要记录详细日志以便排查问题
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error(ResultCode.ERROR);
    }

    /**
     * Throwable 级别异常 - 需要在意的严重异常
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleThrowable(Throwable e) {
        log.error("严重异常 (Throwable): ", e);
        return Result.error(ResultCode.ERROR);
    }
}
