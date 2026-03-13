package com.mtd.demo.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mtd.demo.constant.StrConstant;
import com.mtd.demo.entity.User;
import com.mtd.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 测试类
 */
@SpringBootTest
public class RedisTestControllerTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private UserService userService;

    @Test
    public void testRedisOperations() {
        // 测试 1：查询数据库
        User user1 = userService.getOne(Wrappers.<User>lambdaQuery().last(StrConstant.LIMIT_1));
        System.out.println("从数据库查询的用户：" + user1);
        
        // 测试 2：存储到 Redis
        redisTemplate.opsForValue().set("user1", user1);
        
        // 测试 3：从 Redis 读取
        Object user2 = redisTemplate.opsForValue().get("user1");
        System.out.println("从 Redis 读取的用户：" + user2);
        
        // 验证数据一致性
        if (user1 != null && user2 != null) {
            System.out.println("测试通过！");
        }
    }
}