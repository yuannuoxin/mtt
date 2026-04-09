package com.mtd.mfl.config;

import com.mtd.mfl.entity.MyBaseEntity;
import com.mybatisflex.annotation.UpdateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyUpdateListener implements UpdateListener {

    @Autowired
    private MyUserIdProvider myUserIdProvider;

    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof MyBaseEntity baseEntity) {
            LocalDateTime now = LocalDateTime.now();
            baseEntity.setUpdateBy(myUserIdProvider.getUserId());
            baseEntity.setUpdateTime(now);
        }
    }
}
