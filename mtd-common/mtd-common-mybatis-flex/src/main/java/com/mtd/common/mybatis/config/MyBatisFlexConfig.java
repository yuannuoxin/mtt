package com.mtd.common.mybatis.config;

import com.mtd.common.core.provider.UserIdProvider;
import com.mtd.common.mybatis.base.BaseEntity;
import com.mtd.common.mybatis.handler.MyMetaObjectHandler;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.audit.AuditMessage;
import com.mybatisflex.core.audit.MessageFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

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
        AuditManager.setMessageCollector(auditMessage ->
                System.out.println("[SQL审计] 用户: " + auditMessage.getUser() +
                        " | SQL: " + auditMessage.getFullSql() +
                        " | 耗时: " + auditMessage.getElapsedTime() + "ms")
        );

        FlexGlobalConfig config = FlexGlobalConfig.getDefaultConfig();
        config.registerInsertListener(myInsertListener, BaseEntity.class);
        config.registerUpdateListener(myUpdateListener, BaseEntity.class);
    }
}
