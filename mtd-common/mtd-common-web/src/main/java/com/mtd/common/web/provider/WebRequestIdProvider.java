package com.mtd.common.web.provider;

import com.mtd.common.core.constant.StrCommonConstant;
import com.mtd.common.core.provider.RequestIdProvider;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * RequestID 提供者实现
 * 从 MDC 上下文中获取当前请求的 RequestID
 */
@Component
public class WebRequestIdProvider implements RequestIdProvider {
    
    @Override
    public String getRequestId() {
        return MDC.get(StrCommonConstant.MDC_KEY);
    }
}
