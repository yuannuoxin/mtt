package com.mtd.tt.controller;

import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.Result;
import com.mtd.tt.model.req.ArticleListReq;
import com.mtd.tt.model.req.AcceptAnswerReq;
import com.mtd.tt.model.req.CreatePostReq;
import com.mtd.tt.model.req.PollVoteReq;
import com.mtd.tt.model.req.HomeTimelineReq;
import com.mtd.tt.model.req.PostIdReq;
import com.mtd.tt.model.req.PostRepliesReq;
import com.mtd.tt.model.req.SearchPostReq;
import com.mtd.tt.model.req.ThreadReq;
import com.mtd.tt.model.req.UnlockPasswordReq;
import com.mtd.tt.model.req.UserPostsReq;
import com.mtd.tt.model.req.RelationReq;
import com.mtd.tt.model.resp.PostResp;
import com.mtd.tt.model.resp.RelationResp;
import com.mtd.tt.model.resp.ThreadResp;
import com.mtd.tt.service.PostService;
import com.mtd.tt.service.HideContentService;
import com.mtd.tt.service.PostInteractService;
import com.mtd.tt.util.LoginUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final HideContentService hideContentService;
    private final PostInteractService postInteractService;

    @Operation(summary = "创建帖子（original/reply/quote/repost）")
    @PostMapping("/create")
    public Result<PostResp> create(@RequestBody CreatePostReq req) {
        return Result.success(postService.createPost(LoginUtil.currentUserId(), req));
    }

    @Operation(summary = "删除帖子")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody PostIdReq req) {
        postService.deletePost(LoginUtil.currentUserId(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "帖子详情")
    @PostMapping("/detail")
    public Result<PostResp> detail(@RequestBody PostIdReq req) {
        return Result.success(postService.postDetail(req.getPostId(), LoginUtil.currentUserId()));
    }

    @Operation(summary = "积分解锁隐藏内容")
    @PostMapping("/unlock")
    public Result<PostResp> unlock(@RequestBody PostIdReq req) {
        return Result.success(postService.unlockByPoints(LoginUtil.currentUserId(), req.getPostId()));
    }

    @Operation(summary = "密码解锁隐藏内容（游客可用）")
    @PostMapping("/unlockPassword")
    public Result<PostResp> unlockPassword(@RequestBody UnlockPasswordReq req,
                                           HttpServletRequest request,
                                           HttpServletResponse response) {
        Long userId = LoginUtil.currentUserId();
        String guestToken = hideContentService.ensureGuestToken(request, response, userId == null);
        return Result.success(postService.unlockByPassword(
                userId, req.getPostId(), req.getPassword(), guestToken));
    }

    @Operation(summary = "某用户的帖子列表")
    @PostMapping("/userPosts")
    public Result<PageResponse<PostResp>> userPosts(@RequestBody UserPostsReq req) {
        return Result.success(postService.userPosts(req.getUserId(), req, req.getPostType(), LoginUtil.currentUserId(), req.getMaxId(), req.getMediaType()));
    }

    @Operation(summary = "首页关注时间线（关注的人 + 自己）")
    @PostMapping("/homeTimeline")
    public Result<PageResponse<PostResp>> homeTimeline(@RequestBody HomeTimelineReq req) {
        return Result.success(postService.homeTimeline(LoginUtil.currentUserId(), req, req.getMaxId(), req.getMediaType()));
    }

    @Operation(summary = "全量最新推文（不区分关注，类似推特 Latest）")
    @PostMapping("/globalTimeline")
    public Result<PageResponse<PostResp>> globalTimeline(@RequestBody HomeTimelineReq req) {
        return Result.success(postService.globalTimeline(LoginUtil.currentUserId(), req, req.getMaxId(), req.getMediaType()));
    }

    @Operation(summary = "某帖子的回复列表")
    @PostMapping("/replies")
    public Result<PageResponse<PostResp>> replies(@RequestBody PostRepliesReq req) {
        return Result.success(postService.replies(req.getPostId(), LoginUtil.currentUserId(), req));
    }

    @Operation(summary = "详情页对话视图（上文链 + 直接回复）")
    @PostMapping("/thread")
    public Result<ThreadResp> thread(@RequestBody ThreadReq req) {
        return Result.success(postService.thread(req, LoginUtil.currentUserId()));
    }

    @Operation(summary = "引用 / 转推列表")
    @PostMapping("/relations")
    public Result<RelationResp> relations(@RequestBody RelationReq req) {
        return Result.success(postService.relations(req.getPostId(), req.getType(), req.getSort(),
                req.getPageNum(), req.getPageSize(), LoginUtil.currentUserId()));
    }

    @Operation(summary = "文章列表（发现 / 我的作品）")
    @PostMapping("/articles")
    public Result<PageResponse<PostResp>> articles(@RequestBody ArticleListReq req) {
        return Result.success(postService.articles(req, LoginUtil.currentUserId()));
    }

    @Operation(summary = "搜索帖子")
    @PostMapping("/search")
    public Result<PageResponse<PostResp>> search(@RequestBody SearchPostReq req) {
        return Result.success(postService.search(req.getKeyword(), LoginUtil.currentUserId(), req, req.getMaxId()));
    }

    @Operation(summary = "上报已看（列表页点图片查看原图 / 播放视频音频）")
    @PostMapping("/view")
    public Result<Void> view(@RequestBody PostIdReq req) {
        postService.recordView(req.getPostId());
        return Result.success();
    }

    @Operation(summary = "点赞")
    @PostMapping("/like")
    public Result<Void> like(@RequestBody PostIdReq req) {
        postService.like(LoginUtil.currentUserId(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "取消点赞")
    @PostMapping("/unlike")
    public Result<Void> unlike(@RequestBody PostIdReq req) {
        postService.unlike(LoginUtil.currentUserId(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "收藏帖子")
    @PostMapping("/bookmark")
    public Result<Void> bookmark(@RequestBody PostIdReq req) {
        postService.bookmark(LoginUtil.currentUserId(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @PostMapping("/unbookmark")
    public Result<Void> unbookmark(@RequestBody PostIdReq req) {
        postService.unbookmark(LoginUtil.currentUserId(), req.getPostId());
        return Result.success();
    }

    @Operation(summary = "我的收藏列表")
    @PostMapping("/myBookmarks")
    public Result<PageResponse<PostResp>> myBookmarks(@RequestBody HomeTimelineReq req) {
        return Result.success(postService.myBookmarks(LoginUtil.currentUserId(), req));
    }

    // ===== 六类互动：投票 / 问答采纳 / 悬赏结算 / 抽奖开奖 =====

    @Operation(summary = "投票")
    @PostMapping("/poll/vote")
    public Result<java.util.Map<String, Object>> pollVote(@RequestBody PollVoteReq req) {
        return Result.success(postInteractService.vote(
                LoginUtil.currentUserId(), req.getPostId(), req.getOptionIndexes()));
    }

    @Operation(summary = "结束投票（仅楼主/管理员）")
    @PostMapping("/poll/close")
    public Result<java.util.Map<String, Object>> pollClose(@RequestBody PostIdReq req) {
        return Result.success(postInteractService.closePoll(LoginUtil.currentUserId(), req.getPostId()));
    }

    @Operation(summary = "采纳回答（问答/悬赏，仅楼主/管理员）")
    @PostMapping("/question/accept")
    public Result<java.util.Map<String, Object>> acceptAnswer(@RequestBody AcceptAnswerReq req) {
        return Result.success(postInteractService.acceptAnswer(
                LoginUtil.currentUserId(), req.getPostId(), req.getReplyId()));
    }

    @Operation(summary = "重新打开问答（仅楼主/管理员）")
    @PostMapping("/question/reopen")
    public Result<java.util.Map<String, Object>> reopenQuestion(@RequestBody PostIdReq req) {
        return Result.success(postInteractService.reopenQuestion(LoginUtil.currentUserId(), req.getPostId()));
    }

    @Operation(summary = "悬赏采纳回答（仅楼主/管理员）")
    @PostMapping("/bounty/accept")
    public Result<java.util.Map<String, Object>> acceptBounty(@RequestBody AcceptAnswerReq req) {
        return Result.success(postInteractService.acceptAnswer(
                LoginUtil.currentUserId(), req.getPostId(), req.getReplyId()));
    }

    @Operation(summary = "抽奖开奖（仅楼主/管理员）")
    @PostMapping("/lottery/draw")
    public Result<java.util.Map<String, Object>> drawLottery(@RequestBody PostIdReq req) {
        return Result.success(postInteractService.drawLottery(LoginUtil.currentUserId(), req.getPostId()));
    }
}
