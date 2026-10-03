package com.mtd.tt.mapper;

import com.mtd.tt.model.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.time.OffsetDateTime;
import java.util.UUID;

@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void t1() {
        // 测试 1：查询数据库
        userMapper.selectAll();
    }

    @Rollback(false)
    @Test
    public void t2() {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(UUID.randomUUID().toString());
        userEntity.setEmail(UUID.randomUUID().toString());
        userEntity.setPasswordHash(UUID.randomUUID().toString());
        userEntity.setNickname(UUID.randomUUID().toString());
        userEntity.setAvatar(UUID.randomUUID().toString());
        userEntity.setBanner(UUID.randomUUID().toString());
        userEntity.setBio(UUID.randomUUID().toString());
        userEntity.setStatus(1);
        userEntity.setCreatedAt(OffsetDateTime.now());
        userEntity.setUpdatedAt(OffsetDateTime.now());
        userMapper.insert(userEntity);

        // 测试 1：查询数据库
        userMapper.selectAll();
    }
}