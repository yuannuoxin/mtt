package com.mtd.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.mtd.common.mybatis.base.BaseEntity;
import com.mtd.common.mybatis.user.UserIdProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 自动填充 Java 规范的 4 个字段：createTime, updateTime, createBy, updateBy
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Autowired(required = false)
    private UserIdProvider userIdProvider;

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("========== MyBatis-Plus 自动填充 INSERT ==========");
        log.info("原始对象：{}", metaObject.getOriginalObject());
        
        // 插入时填充所有 4 个字段
        LocalDateTime now = LocalDateTime.now();
        Long userId = getCurrentUserId();
        
        log.info("当前用户 ID: {}", userId);
        
        this.strictInsertFill(metaObject, BaseEntity.Fields.createTime, LocalDateTime.class, now);
        log.info("已填充 createTime: {}", now);
        
        this.strictInsertFill(metaObject, BaseEntity.Fields.updateTime, LocalDateTime.class, now);
        log.info("已填充 updateTime: {}", now);
        
        // 只有在有用户 ID 时才填充 createBy 和 updateBy
        if (userId != null) {
            this.strictInsertFill(metaObject, BaseEntity.Fields.createBy, Long.class, userId);
            log.info("已填充 createBy: {}", userId);
            
            this.strictInsertFill(metaObject, BaseEntity.Fields.updateBy, Long.class, userId);
            log.info("已填充 updateBy: {}", userId);
        } else {
            log.warn("未获取到当前用户 ID，跳过 createBy 和 updateBy 的填充");
        }
        
        log.info("===================================================");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("MyBatis-Plus 自动填充更新字段");
        
        // 更新时只填充 updateTime 和 updateBy
        this.strictUpdateFill(metaObject, BaseEntity.Fields.updateTime, LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, BaseEntity.Fields.updateBy, Long.class, getCurrentUserId());
    }

    /**
     * 获取当前登录用户 ID
     * 如果存在 UserIdProvider 实现则使用，否则返回 null
     */
    private Long getCurrentUserId() {
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
