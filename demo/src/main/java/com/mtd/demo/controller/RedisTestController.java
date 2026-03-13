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

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "测试 RedisTemplate - 设置对象")
    @PostMapping("/template/set")
    public String redisTemplateSet(@RequestParam String key, @RequestParam Object value) {
        redisTemplate.opsForValue().set(key, value);
        return "success";
    }

    @Operation(summary = "测试 RedisTemplate - 获取对象")
    @GetMapping("/template/get")
    public Object redisTemplateGet(@RequestParam String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Operation(summary = "测试 RedisTemplate - 设置带过期时间")
    @PostMapping("/template/setWithExpire")
    public String redisTemplateSetWithExpire(
            @RequestParam String key, 
            @RequestParam Object value,
            @RequestParam long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        return "success";
    }

    @Operation(summary = "测试 RedisTemplate - Hash 操作")
    @PostMapping("/template/hash")
    public String redisTemplateHash(
            @RequestParam String hashKey,
            @RequestParam String field,
            @RequestParam Object value) {
        redisTemplate.opsForHash().put(hashKey, field, value);
        return "success";
    }

    @Operation(summary = "测试 RedisTemplate - 获取 Hash")
    @GetMapping("/template/hash")
    public Object redisTemplateHashGet(
            @RequestParam String hashKey,
            @RequestParam String field) {
        return redisTemplate.opsForHash().get(hashKey, field);
    }

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


    @Operation(summary = "检查 Key 是否存在")
    @GetMapping("/hasKey")
    public Boolean hasKey(@RequestParam String key) {
        return redisTemplate.hasKey(key);
    }

    @Operation(summary = "设置过期时间")
    @PostMapping("/expire")
    public Boolean expire(
            @RequestParam String key,
            @RequestParam long seconds) {
        return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }
}
