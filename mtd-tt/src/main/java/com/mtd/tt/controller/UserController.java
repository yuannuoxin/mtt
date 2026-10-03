package com.mtd.tt.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.Result;
import com.mtd.tt.model.req.BlockReq;
import com.mtd.tt.model.req.FollowReq;
import com.mtd.tt.model.req.SearchUserReq;
import com.mtd.tt.model.req.UpdateProfileReq;
import com.mtd.tt.model.req.UserIdReq;
import com.mtd.tt.model.req.UserListReq;
import com.mtd.tt.model.resp.UserProfileResp;
import com.mtd.tt.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：资料 / 关注 / 粉丝 / 拉黑 / 搜索
 */
@Tag(name = "用户", description = "用户资料、关注、粉丝、拉黑、搜索")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取用户资料")
    @PostMapping("/profile")
    @SaCheckLogin
    public Result<UserProfileResp> profile(@RequestBody UserIdReq req) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.getProfile(req.getUserId(), currentUserId));
    }

    @Operation(summary = "修改个人资料")
    @PostMapping("/updateProfile")
    @SaCheckLogin
    public Result<UserProfileResp> updateProfile(@RequestBody UpdateProfileReq req) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.updateProfile(currentUserId, req));
    }

    @Operation(summary = "关注用户")
    @PostMapping("/follow")
    @SaCheckLogin
    public Result<Void> follow(@RequestBody FollowReq req) {
        userService.follow(StpUtil.getLoginIdAsLong(), req.getFolloweeId());
        return Result.success();
    }

    @Operation(summary = "取消关注")
    @PostMapping("/unfollow")
    @SaCheckLogin
    public Result<Void> unfollow(@RequestBody FollowReq req) {
        userService.unfollow(StpUtil.getLoginIdAsLong(), req.getFolloweeId());
        return Result.success();
    }

    @Operation(summary = "某用户关注的用户列表")
    @PostMapping("/following")
    @SaCheckLogin
    public Result<PageResponse<UserProfileResp>> following(@RequestBody UserListReq req) {
        return Result.success(userService.followingList(req.getUserId(), req, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "某用户的粉丝列表")
    @PostMapping("/followers")
    @SaCheckLogin
    public Result<PageResponse<UserProfileResp>> followers(@RequestBody UserListReq req) {
        return Result.success(userService.followerList(req.getUserId(), req, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "拉黑用户")
    @PostMapping("/block")
    @SaCheckLogin
    public Result<Void> block(@RequestBody BlockReq req) {
        userService.block(StpUtil.getLoginIdAsLong(), req.getBlockedUserId());
        return Result.success();
    }

    @Operation(summary = "取消拉黑")
    @PostMapping("/unblock")
    @SaCheckLogin
    public Result<Void> unblock(@RequestBody BlockReq req) {
        userService.unblock(StpUtil.getLoginIdAsLong(), req.getBlockedUserId());
        return Result.success();
    }

    @Operation(summary = "搜索用户")
    @PostMapping("/search")
    @SaCheckLogin
    public Result<PageResponse<UserProfileResp>> search(@RequestBody SearchUserReq req) {
        return Result.success(userService.search(req.getKeyword(), req, StpUtil.getLoginIdAsLong()));
    }
}
