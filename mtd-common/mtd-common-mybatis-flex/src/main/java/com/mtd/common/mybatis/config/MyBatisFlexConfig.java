package com.mtd.common.mybatis.config;

import ch.qos.logback.core.util.StringUtil;
import com.mtd.common.mybatis.base.BaseEntity;
import com.mtd.common.mybatis.handler.MyMetaObjectHandler;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.audit.AuditMessage;
import com.mybatisflex.core.audit.MessageFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MyBatisFlexConfig {

    @Autowired
    private MyMetaObjectHandler myMetaObjectHandler;

    @Autowired
    private MyUpdateListener myUpdateListener;
    @Autowired
    private MyInsertListener myInsertListener;

    @PostConstruct
    public void init() {
        AuditManager.setAuditEnable(true);

        MessageFactory factory = () -> {
            AuditMessage message = new AuditMessage();
            message.setUser("用户:" + myMetaObjectHandler.getCurrentUserId());
            return message;
        };
        AuditManager.setMessageFactory(factory);
        AuditManager.setMessageCollector(auditMessage -> log.info("[SQL审计] {} 用户: {} | SQL: {} | 耗时: {}", StringUtil.nullStringToEmpty(MDC.get("requestId")), auditMessage.getUser(), auditMessage.getFullSql(), auditMessage.getElapsedTime())
        );

        FlexGlobalConfig config = FlexGlobalConfig.getDefaultConfig();
        config.registerInsertListener(myInsertListener, BaseEntity.class);
        config.registerUpdateListener(myUpdateListener, BaseEntity.class);
    }
}
