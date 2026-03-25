package com.mtd.iam.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mtd.common.core.result.Result;
import com.mtd.iam.entity.User;
import com.mtd.iam.mapper.UserMapper;
import com.mtd.iam.resp.UserInfoResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "用户")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/userInfo")
    @SaCheckLogin
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserInfoResp> userInfo() {
        long loginIdAsLong = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(loginIdAsLong);

        UserInfoResp resp = new UserInfoResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setStatus(user.getStatus());

        return Result.success(resp);
    }
}
