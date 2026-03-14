package com.mtd.common.web.aspect;

import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求日志切面
 * 统一拦截所有 Controller 的入参和响应
 */
@Slf4j
@Aspect
@Component
public class RequestLogAspect {


    /**
     * 定义切点：所有 Controller 层方法
     */
    @Pointcut("execution(* com..controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 前置通知：记录请求信息
     */
    @Before("controllerPointcut()")
    public void before(JoinPoint joinPoint) {
        try {
            // 获取请求上下文
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // 获取基本信息
            String method = request != null ? request.getMethod() : null;
            String url = request != null ? request.getRequestURL().toString() : null;
            String className = joinPoint.getSignature().getDeclaringTypeName();
            String methodName = joinPoint.getSignature().getName();
            String requestId = org.slf4j.MDC.get("requestId");
            String ip = request != null ? JakartaServletUtil.getClientIP(request) : null;

            // 记录日志
            log.info("\n========== 请求开始 ==========");
            log.info("RequestID: {}", requestId);
            log.info("接口：{}.{}", className, methodName);
            log.info("URL: {}", url);
            log.info("方法：{}", method);
            log.info("参数：{}", JSONUtil.toJsonStr(joinPoint.getArgs()));
            log.info("IP: {}", ip);
        } catch (Exception e) {
            log.info("记录请求日志异常：", e);
        }
    }

    /**
     * 后置通知：记录响应信息
     */
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        try {
            // 获取 RequestID
            String requestId = org.slf4j.MDC.get("requestId");

            // 序列化响应结果
            String resultJson = null;
            if (result != null) {
                try {
                    resultJson = JSONUtil.toJsonStr(result);
                } catch (Exception e) {
                    resultJson = String.valueOf(result);
                }
            }

            // 记录日志
            log.info("RequestID: {}", requestId);
            log.info("响应状态：SUCCESS");
            if (resultJson != null) {
                if (resultJson.length() > 1000) {
                    log.info("响应：{}... (共 {} 字符)", resultJson.substring(0, 1000), resultJson.length());
                } else {
                    log.info("响应：{}", resultJson);
                }
            }
            log.info("========== 请求结束 ==========\n");
        } catch (Exception e) {
            log.info("记录请求日志异常：", e);
        }
    }

    /**
     * 异常通知：记录异常信息
     */
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Throwable e) {
        try {
            String requestId = org.slf4j.MDC.get("requestId");
            String exceptionMessage = e.getMessage();
            String exceptionType = e.getClass().getName();

            log.error("RequestID: {}", requestId);
            log.error("响应状态：ERROR");
            log.error("异常信息：{}", exceptionMessage);
            log.error("异常类型：{}", exceptionType);
            log.error("========== 请求结束（异常） ==========\n");
        } catch (Exception ex) {
            log.info("记录请求日志异常：", e);
        }
    }
}
