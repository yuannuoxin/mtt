package com.mtd.mjq.controller;

import com.mtd.common.core.result.Result;
import com.mtd.mjq.entity.SysUser;
import com.mtd.mjq.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @PostMapping
    public Result<SysUser> create(@RequestBody SysUser user) {
        return Result.success(userService.create(user));
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping
    public Result<List<SysUser>> list() {
        return Result.success(userService.list());
    }

    @PutMapping
    public Result<SysUser> update(@RequestBody SysUser user) {
        return Result.success(userService.update(user));
    }
    @PutMapping("update2")
    public Result<SysUser> update2(@RequestBody SysUser user) {
        return Result.success(userService.update2(user));
    }

    @PutMapping("updateTest")
    public Result<SysUser> updateTest() {
        return Result.success(userService.updateTest());
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.deleteById(id);
        return Result.success();
    }
}
