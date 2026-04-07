package com.mtd.mfl.config;

import com.mtd.mfl.entity.MyBaseEntity;
import com.mybatisflex.annotation.UpdateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyUpdateListener implements UpdateListener {

    @Autowired
    private MyUserIdProvider myUserIdProvider;

    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof MyBaseEntity) {
            MyBaseEntity baseEntity = (MyBaseEntity) entity;
            baseEntity.setUpdateBy(myUserIdProvider.getUserId());
        }
    }
}
