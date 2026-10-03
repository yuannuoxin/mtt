package com.mtd.tt.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.Result;
import com.mtd.tt.model.req.CreatePostReq;
import com.mtd.tt.model.req.HomeTimelineReq;
import com.mtd.tt.model.req.PostIdReq;
import com.mtd.tt.model.req.PostRepliesReq;
import com.mtd.tt.model.req.SearchPostReq;
import com.mtd.tt.model.req.UserPostsReq;
import com.mtd.tt.model.resp.PostResp;
import com.mtd.tt.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子接口：发帖 / 详情 / 时间线 / 搜索 / 点赞 / 收藏 / 回复 / 转发
 */
@Tag(name = "帖子", description = "发帖、详情、时间线、搜索、点赞、收藏")
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "创建帖子（original/reply/quote/repost）")
    @PostMapping("/create")
    @SaCheckLogin
    public Result<PostResp> create(@RequestBody CreatePostReq req) {
        return Result.success(postService.createPost(StpUtil.getLoginIdAsLong(), req));
    }

    @Operation(summary = "删除帖子")
    @PostMapping("/delete")
    @SaCheckLogin
    public Result<Void> delete(@RequestBody PostIdReq req) {
        postService.deletePost(StpUtil.getLoginIdAsLong(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "帖子详情")
    @PostMapping("/detail")
    @SaCheckLogin
    public Result<PostResp> detail(@RequestBody PostIdReq req) {
        return Result.success(postService.postDetail(req.getPostId(), StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "某用户的帖子列表")
    @PostMapping("/userPosts")
    @SaCheckLogin
    public Result<PageResponse<PostResp>> userPosts(@RequestBody UserPostsReq req) {
        return Result.success(postService.userPosts(req.getUserId(), req, req.getPostType(), StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "首页关注时间线")
    @PostMapping("/homeTimeline")
    @SaCheckLogin
    public Result<PageResponse<PostResp>> homeTimeline(@RequestBody HomeTimelineReq req) {
        return Result.success(postService.homeTimeline(StpUtil.getLoginIdAsLong(), req));
    }

    @Operation(summary = "某帖子的回复列表")
    @PostMapping("/replies")
    @SaCheckLogin
    public Result<PageResponse<PostResp>> replies(@RequestBody PostRepliesReq req) {
        return Result.success(postService.replies(req.getPostId(), StpUtil.getLoginIdAsLong(), req));
    }

    @Operation(summary = "搜索帖子")
    @PostMapping("/search")
    @SaCheckLogin
    public Result<PageResponse<PostResp>> search(@RequestBody SearchPostReq req) {
        return Result.success(postService.search(req.getKeyword(), StpUtil.getLoginIdAsLong(), req));
    }

    @Operation(summary = "点赞")
    @PostMapping("/like")
    @SaCheckLogin
    public Result<Void> like(@RequestBody PostIdReq req) {
        postService.like(StpUtil.getLoginIdAsLong(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "取消点赞")
    @PostMapping("/unlike")
    @SaCheckLogin
    public Result<Void> unlike(@RequestBody PostIdReq req) {
        postService.unlike(StpUtil.getLoginIdAsLong(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "收藏帖子")
    @PostMapping("/bookmark")
    @SaCheckLogin
    public Result<Void> bookmark(@RequestBody PostIdReq req) {
        postService.bookmark(StpUtil.getLoginIdAsLong(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @PostMapping("/unbookmark")
    @SaCheckLogin
    public Result<Void> unbookmark(@RequestBody PostIdReq req) {
        postService.unbookmark(StpUtil.getLoginIdAsLong(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "我的收藏列表")
    @PostMapping("/myBookmarks")
    @SaCheckLogin
    public Result<PageResponse<PostResp>> myBookmarks(@RequestBody HomeTimelineReq req) {
        return Result.success(postService.myBookmarks(StpUtil.getLoginIdAsLong(), req));
    }
}
