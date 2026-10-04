package com.mtd.tt.controller;

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
import com.mtd.tt.util.LoginUtil;
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
    public Result<UserProfileResp> profile(@RequestBody UserIdReq req) {
        Long currentUserId = LoginUtil.currentUserId();
        return Result.success(userService.getProfile(req.getUserId(), currentUserId));
    }

    @Operation(summary = "修改个人资料")
    @PostMapping("/updateProfile")
    public Result<UserProfileResp> updateProfile(@RequestBody UpdateProfileReq req) {
        Long currentUserId = LoginUtil.currentUserId();
        return Result.success(userService.updateProfile(currentUserId, req));
    }

    @Operation(summary = "关注用户")
    @PostMapping("/follow")
    public Result<Void> follow(@RequestBody FollowReq req) {
        userService.follow(LoginUtil.currentUserId(), req.getFolloweeId());
        return Result.success();
    }

    @Operation(summary = "取消关注")
    @PostMapping("/unfollow")
    public Result<Void> unfollow(@RequestBody FollowReq req) {
        userService.unfollow(LoginUtil.currentUserId(), req.getFolloweeId());
        return Result.success();
    }

    @Operation(summary = "某用户关注的用户列表")
    @PostMapping("/following")
    public Result<PageResponse<UserProfileResp>> following(@RequestBody UserListReq req) {
        return Result.success(userService.followingList(req.getUserId(), req, LoginUtil.currentUserId()));
    }

    @Operation(summary = "某用户的粉丝列表")
    @PostMapping("/followers")
    public Result<PageResponse<UserProfileResp>> followers(@RequestBody UserListReq req) {
        return Result.success(userService.followerList(req.getUserId(), req, LoginUtil.currentUserId()));
    }

    @Operation(summary = "拉黑用户")
    @PostMapping("/block")
    public Result<Void> block(@RequestBody BlockReq req) {
        userService.block(LoginUtil.currentUserId(), req.getBlockedUserId());
        return Result.success();
    }

    @Operation(summary = "取消拉黑")
    @PostMapping("/unblock")
    public Result<Void> unblock(@RequestBody BlockReq req) {
        userService.unblock(LoginUtil.currentUserId(), req.getBlockedUserId());
        return Result.success();
    }

    @Operation(summary = "搜索用户")
    @PostMapping("/search")
    public Result<PageResponse<UserProfileResp>> search(@RequestBody SearchUserReq req) {
        return Result.success(userService.search(req.getKeyword(), req, LoginUtil.currentUserId()));
    }
}
