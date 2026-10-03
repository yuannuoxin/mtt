package com.mtd.tt.controller;

import com.mtd.common.core.result.Result;
import com.mtd.tt.mapper.UserMapper;
import com.mtd.tt.model.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * tmp2
 */
@Slf4j
@Tag(name = "tmp2")
@RestController
@RequestMapping("/tmp2")
public class Tmp2Controller {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Operation(summary = "t1", description = "t1")
    @PostMapping("/t1")
    public Result<?> t1() {
        return Result.success("t1");
    }

    @Operation(summary = "stringRedisTemplateIncrement", description = "stringRedisTemplateIncrement")
    @PostMapping("/stringRedisTemplateIncrement")
    public Result<?> stringRedisTemplateIncrement() {
        Long increment = stringRedisTemplate.opsForValue().increment("stringRedisTemplate.increment");
        return Result.success(increment);
    }

    @Operation(summary = "t3", description = "t3")
    @PostMapping("/t3")
    public Result<?> t3() {
        List<UserEntity> userEntities = userMapper.selectAll();
        return Result.success(userEntities);
    }
}
