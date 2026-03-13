package com.mtd.demo.advice;

import com.mtd.common.core.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体增强器
 * 在响应写入前，自动添加 RequestID 到 Result 中
 */
@Slf4j
@ControllerAdvice
public class MyResponseBodyAdviceImpl implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 只处理返回类型为 Result 的方法
        return returnType.getParameterType().equals(Result.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                   MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request,
                                   ServerHttpResponse response) {
        // 从 MDC 中获取 RequestID
        String requestId = MDC.get("requestId");

        if (body instanceof Result && requestId != null) {
            Result<?> result = (Result<?>) body;
            result.setRequestId(requestId);
        }
        
        return body;
    }
}
