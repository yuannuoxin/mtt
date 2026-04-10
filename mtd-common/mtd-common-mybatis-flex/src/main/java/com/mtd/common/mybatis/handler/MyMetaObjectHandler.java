package com.mtd.common.mybatis.handler;

import com.mtd.common.core.provider.UserIdProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class MyMetaObjectHandler {

    @Autowired(required = false)
    private UserIdProvider userIdProvider;

    /**
     * 获取当前登录用户 ID
     * 如果存在 UserIdProvider 实现则使用，否则返回 null
     */
    public Long getCurrentUserId() {
        if (userIdProvider != null) {
            try {
                return userIdProvider.getUserId();
            } catch (Exception e) {
                log.warn("获取当前用户 ID 失败：{}", e.getMessage());
            }
        }
        return null;
    }
}
