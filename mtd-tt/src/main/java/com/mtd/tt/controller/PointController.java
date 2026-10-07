package com.mtd.tt.controller;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.model.resp.CheckinResp;
import com.mtd.tt.model.resp.PointRecordResp;
import com.mtd.tt.service.PointService;
import com.mtd.tt.util.LoginUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 积分接口：每日签到、积分记录查询。
 * 积分余额随用户资料一起返回（见 userProfile.points）。
 */
@Tag(name = "积分", description = "每日签到与积分记录")
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(summary = "每日签到（送积分）")
    @PostMapping("/checkin")
    public Result<CheckinResp> checkin() {
        Long userId = requireLogin();
        return Result.success(pointService.checkin(userId));
    }

    @Operation(summary = "查询当前用户积分记录（分页）")
    @PostMapping("/records")
    public Result<PageResponse<PointRecordResp>> records(@RequestBody(required = false) PageRequest page) {
        Long userId = requireLogin();
        PageRequest p = page == null ? new PageRequest() : page;
        return Result.success(pointService.records(userId, p));
    }

    private Long requireLogin() {
        Long userId = LoginUtil.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        return userId;
    }
}
