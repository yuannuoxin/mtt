package com.mtd.common.mybatis.user;

/**
 * 用户 ID 提供者接口
 * 用于从上下文中获取当前登录用户 ID
 */
@FunctionalInterface
public interface UserIdProvider {
    
    /**
     * 获取当前登录用户 ID
     * @return 用户 ID，如果未登录则返回 null
     */
    Long getUserId();
}
