package com.mtd.mfl.config;

import com.mybatisflex.annotation.InsertListener;
import com.mtd.mfl.entity.MyBaseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyInsertListener implements InsertListener {

    @Autowired
    private MyUserIdProvider myUserIdProvider;

    @Override
    public void onInsert(Object entity) {
        if (entity instanceof MyBaseEntity) {
            MyBaseEntity baseEntity = (MyBaseEntity) entity;
            
            if (baseEntity.getCreateBy() == null) {
                baseEntity.setCreateBy(myUserIdProvider.getUserId());
            }
            if (baseEntity.getUpdateBy() == null) {
                baseEntity.setUpdateBy(myUserIdProvider.getUserId());
            }
            if (baseEntity.getDeleted() == null) {
                baseEntity.setDeleted(0);
            }
        }
    }
}
