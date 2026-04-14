package com.mtd.common.core.provider;

/**
 * RequestID 提供者接口
 * 用于从上下文中获取当前请求的 RequestID
 */
@FunctionalInterface
public interface RequestIdProvider {
    
    /**
     * 获取当前请求的 RequestID
     * @return RequestID，如果不存在则返回 null
     */
    String getRequestId();
}
