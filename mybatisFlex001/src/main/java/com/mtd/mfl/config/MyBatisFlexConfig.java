package com.mtd.mfl.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class MyBatisFlexConfig {

    @Autowired
    private MyUserIdProvider myUserIdProvider;
    @Autowired
    private MyUpdateListener myUpdateListener;
    @Autowired
    private MyInsertListener myInsertListener;

    @PostConstruct
    public void init() {
        AuditManager.setAuditEnable(true);

        MessageFactory factory = () -> {
            AuditMessage message = new AuditMessage();
            message.setUser("用户-" + myUserIdProvider.getUserId());
            return message;
        };

        AuditManager.setMessageFactory(factory);
        AuditManager.setMessageCollector(auditMessage -> 
            System.out.println("[SQL审计] 用户: " + auditMessage.getUser() + 
                             " | SQL: " + auditMessage.getFullSql() +
                             " | 耗时: " + auditMessage.getElapsedTime() + "ms")
        );

        FlexGlobalConfig config = FlexGlobalConfig.getDefaultConfig();
        config.registerInsertListener(myInsertListener, com.mtd.mfl.entity.MyBaseEntity.class);
        config.registerUpdateListener(myUpdateListener, com.mtd.mfl.entity.MyBaseEntity.class);
    }
}
