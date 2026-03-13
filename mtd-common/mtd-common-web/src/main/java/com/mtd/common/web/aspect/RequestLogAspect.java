package com.mtd.common.web.aspect;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 请求日志切面
 * 统一拦截所有 Controller 的入参和响应
 * 使用异步日志，确保不影响接口性能
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequestLogAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 异步日志线程池
    private static final ExecutorService LOG_EXECUTOR = new ThreadPoolExecutor(
            2,                      // 核心线程数
            5,                      // 最大线程数
            60L,                    // 空闲超时
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),  // 队列容量
            r -> {
                Thread thread = new Thread(r);
                thread.setName("async-log-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时由调用线程执行
    );

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
        // 在主线程中获取请求上下文信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (attributes != null) {
            request = attributes.getRequest();
        }

        String method = null;
        if (request != null) {
            method = request.getMethod();
        }
        String url = null;
        if (request != null) {
            url = request.getRequestURL().toString();
        }
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        // 在主线程中获取 RequestID（从 MDC 中获取）
        String requestId = org.slf4j.MDC.get("requestId");
        
        // 获取入参 JSON
        Object[] args = joinPoint.getArgs();
        StringBuilder paramsBuilder = new StringBuilder();
        if (args.length > 0) {
            paramsBuilder.append("入参：\n");
            for (int i = 0; i < args.length; i++) {
                try {
                    String paramJson = objectMapper.writeValueAsString(args[i]);
                    paramsBuilder.append("  参数 ").append(i).append(": ").append(paramJson).append("\n");
                } catch (Exception e) {
                    paramsBuilder.append("  参数 ").append(i).append(": ").append(args[i]).append("\n");
                }
            }
        }
        String ip = null;
        if (request != null) {
            ip = JakartaServletUtil.getClientIP(request);
        }
        
        // 将可变变量转换为 final，以便在 lambda 中使用
        final String finalRequestId = requestId;
        final String finalMethod = method;
        final String finalUrl = url;
        final String finalIp = ip;
        final String finalParams = paramsBuilder.toString().trim();

        // 异步记录日志，不阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                log.info("\n========== 请求开始 ==========");
                log.info("RequestID: {}", finalRequestId);
                log.info("接口：{}.{}", className, methodName);
                log.info("URL: {}", finalUrl);
                log.info("方法：{}", finalMethod);
                log.info("IP: {}", finalIp);
                if (!finalParams.isEmpty()) {
                    log.info("{}", finalParams);
                }
            } catch (Exception e) {
                log.error("记录请求日志失败", e);
            }
        }, LOG_EXECUTOR);
    }

    /**
     * 后置通知：记录响应信息
     * 使用异步方式，不阻塞主线程
     */
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        // 在主线程中获取 RequestID
        String requestId = org.slf4j.MDC.get("requestId");
        
        // 在主线程中序列化响应结果
        String resultJson = null;
        if (result != null) {
            try {
                resultJson = objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                resultJson = String.valueOf(result);
            }
        }
        final String finalResultJson = resultJson;

        // 异步记录日志
        CompletableFuture.runAsync(() -> {
            try {
                log.info("RequestID: {}", requestId);
                log.info("响应状态：SUCCESS");
                if (finalResultJson != null) {
                    // 如果响应太长，只打印前 1000 个字符
                    if (finalResultJson.length() > 1000) {
                        log.info("响应：{}... (共 {} 字符)", finalResultJson.substring(0, 1000), finalResultJson.length());
                    } else {
                        log.info("响应：{}", finalResultJson);
                    }
                }
                log.info("========== 请求结束 ==========\n");
            } catch (Exception e) {
                log.error("记录响应日志失败", e);
            }
        }, LOG_EXECUTOR);
    }

    /**
     * 异常通知：记录异常信息
     */
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Throwable e) {
        // 在主线程中获取 RequestID
        String requestId = org.slf4j.MDC.get("requestId");
        final String finalRequestId = requestId;
        
        // 在主线程中获取异常信息
        final String exceptionMessage = e.getMessage();
        final String exceptionType = e.getClass().getName();
        
        // 异步记录日志
        CompletableFuture.runAsync(() -> {
            try {
                log.error("RequestID: {}", finalRequestId);
                log.error("响应状态：ERROR");
                log.error("异常信息：{}", exceptionMessage);
                log.error("异常类型：{}", exceptionType);
                log.error("========== 请求结束（异常） ==========\n");
            } catch (Exception ex) {
                log.error("记录异常日志失败", ex);
            }
        }, LOG_EXECUTOR);
    }
}
