package com.mtd.common.mybatis.config;

import com.mtd.common.mybatis.base.BaseEntity;
import com.mtd.common.mybatis.base.BaseEntity2;
import com.mtd.common.mybatis.base.BaseEntity3;
import com.mtd.common.mybatis.handler.MyMetaObjectHandler;
import com.mybatisflex.annotation.UpdateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyUpdateListener implements UpdateListener {

    @Autowired
    private MyMetaObjectHandler myMetaObjectHandler;

    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            LocalDateTime now = LocalDateTime.now();
            baseEntity.setUpdateBy(myMetaObjectHandler.getCurrentUserId());
            baseEntity.setUpdateTime(now);
        }

        if (entity instanceof BaseEntity2 baseEntity) {
            LocalDateTime now = LocalDateTime.now();
            baseEntity.setUpdateBy(myMetaObjectHandler.getCurrentUserId());
            baseEntity.setUpdateTime(now);
        }
    }
}
