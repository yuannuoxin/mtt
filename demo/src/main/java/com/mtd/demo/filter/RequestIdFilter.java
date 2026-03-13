package com.mtd.demo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * RequestID 过滤器
 * 为每个请求生成唯一的 RequestID，并放入 MDC 日志上下文
 */
@Slf4j
@Order(1) // 确保在最前面执行
@Component
public class RequestIdFilter implements Filter {

    /**
     * RequestID 的 Header 名称
     */
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    
    /**
     * MDC 中的 Key
     */
    private static final String MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // 1. 获取或生成 RequestID
            String requestId = getOrGenerateRequestId(httpRequest);
            
            // 2. 放入 MDC 上下文（这样日志中会自动包含 requestId）
            MDC.put(MDC_KEY, requestId);
            
            // 3. 将 RequestID 放入响应头，方便前端追踪
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
            
            // 4. 继续执行过滤器链
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("RequestID 过滤器异常", e);
            throw e;
        } finally {
            // 5. 清理 MDC（非常重要，防止内存泄漏和线程复用问题）
            MDC.clear();
        }
    }

    /**
     * 获取 RequestID 的策略：
     * 1. 优先从请求头获取（如果前端传递了）
     * 2. 否则生成新的 UUID
     */
    private String getOrGenerateRequestId(HttpServletRequest request) {
        // 尝试从请求头获取（前端可能传递）
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        
        if (requestId == null || requestId.isEmpty()) {
            // 生成新的 RequestID（使用短 UUID，去掉横杠）
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        
        return requestId;
    }
}
