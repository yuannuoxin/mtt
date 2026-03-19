package com.mtd.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Redis 测试接口
 */
@Tag(name = "Redis 测试")
@RestController
@RequestMapping("/test/redis")
@RequiredArgsConstructor
public class RedisTestController {

    private final StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "测试 StringRedisTemplate - 设置字符串")
    @PostMapping("/string/set")
    public String stringRedisTemplateSet(@RequestParam String key, @RequestParam String value) {
        stringRedisTemplate.opsForValue().set(key, value);
        return "success";
    }

    @Operation(summary = "测试 StringRedisTemplate - 获取字符串")
    @GetMapping("/string/get")
    public String stringRedisTemplateGet(@RequestParam String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Operation(summary = "测试 StringRedisTemplate - 自增")
    @PostMapping("/string/increment")
    public Long stringRedisTemplateIncrement(@RequestParam String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    @Operation(summary = "测试 StringRedisTemplate - 自增指定值")
    @PostMapping("/string/incrementBy")
    public Long stringRedisTemplateIncrementBy(
            @RequestParam String key,
            @RequestParam long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }
}
