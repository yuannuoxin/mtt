package com.mtd.tt.controller;

import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.Result;
import com.mtd.tt.model.resp.NotificationResp;
import com.mtd.tt.service.NotificationService;
import com.mtd.tt.util.LoginUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 通知接口：列表 / 已读 / 未读数
 */
@Tag(name = "通知", description = "点赞、评论、关注、转发、提及通知")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "通知列表")
    @PostMapping("/list")
    public Result<PageResponse<NotificationResp>> list(@RequestBody PageRequest req) {
        return Result.success(notificationService.list(LoginUtil.currentUserId(), req));
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/markAllRead")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(LoginUtil.currentUserId());
        return Result.success();
    }

    @Operation(summary = "单条标记已读")
    @PostMapping("/read")
    public Result<Void> read(@RequestBody Map<String, Long> body) {
        notificationService.markRead(LoginUtil.currentUserId(), body.get("id"));
        return Result.success();
    }

    @Operation(summary = "未读通知数量")
    @PostMapping("/unreadCount")
    public Result<Map<String, Long>> unreadCount() {
        return Result.success(Map.of("count", notificationService.unreadCount(LoginUtil.currentUserId())));
    }
}
