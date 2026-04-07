package com.mtd.mjq.service;

import com.mtd.mjq.entity.SysUser;
import com.mtd.mjq.repository.SysUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SysUserServiceTest {

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysUserRepository userRepository;

    @Test
//    @Transactional
    @Rollback(false)
    void testUpdate() {
        SysUser user = new SysUser();
        user.setUsername(UUID.randomUUID().toString());
        user.setPassword("123456");
        user.setNickname("测试用户");
        user.setPhone("13800138000");
        user.setStatus(1);

        user = userService.create(user);
        assertNotNull(user.getId());

        user.setNickname("更新后的昵称");
        user.setEmail("updated@example.com");

        userService.update(user);
    }
}
