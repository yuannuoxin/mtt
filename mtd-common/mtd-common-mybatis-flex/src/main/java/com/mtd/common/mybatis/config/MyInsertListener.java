package com.mtd.common.mybatis.config;

import com.mtd.common.mybatis.base.BaseEntity;
import com.mtd.common.mybatis.base.BaseEntity2;
import com.mtd.common.mybatis.base.BaseEntity3;
import com.mtd.common.mybatis.handler.MyMetaObjectHandler;
import com.mybatisflex.annotation.InsertListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyInsertListener implements InsertListener {

    @Autowired
    private MyMetaObjectHandler myMetaObjectHandler;

    @Override
    public void onInsert(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            LocalDateTime now = LocalDateTime.now();

            if (baseEntity.getCreateBy() == null) {
                baseEntity.setCreateBy(myMetaObjectHandler.getCurrentUserId());
            }
            if (baseEntity.getUpdateBy() == null) {
                baseEntity.setUpdateBy(myMetaObjectHandler.getCurrentUserId());
            }

            if (baseEntity.getCreateTime() == null) {
                baseEntity.setCreateTime(now);
            }
            if (baseEntity.getUpdateTime() == null) {
                baseEntity.setUpdateTime(now);
            }

            if (baseEntity.getDeleted() == null) {
                baseEntity.setDeleted(0);
            }
        }

        if (entity instanceof BaseEntity2 baseEntity) {
            LocalDateTime now = LocalDateTime.now();

            if (baseEntity.getCreateBy() == null) {
                baseEntity.setCreateBy(myMetaObjectHandler.getCurrentUserId());
            }
            if (baseEntity.getUpdateBy() == null) {
                baseEntity.setUpdateBy(myMetaObjectHandler.getCurrentUserId());
            }

            if (baseEntity.getCreateTime() == null) {
                baseEntity.setCreateTime(now);
            }
            if (baseEntity.getUpdateTime() == null) {
                baseEntity.setUpdateTime(now);
            }
        }

        if (entity instanceof BaseEntity3 baseEntity) {
            LocalDateTime now = LocalDateTime.now();

            if (baseEntity.getCreateBy() == null) {
                baseEntity.setCreateBy(myMetaObjectHandler.getCurrentUserId());
            }

            if (baseEntity.getCreateTime() == null) {
                baseEntity.setCreateTime(now);
            }
        }
    }
}
