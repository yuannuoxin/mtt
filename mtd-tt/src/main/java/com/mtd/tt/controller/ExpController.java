package com.mtd.tt.controller;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.request.PageRequest;
import com.mtd.common.core.response.PageResponse;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.model.resp.ExpRecordResp;
import com.mtd.tt.service.ExpService;
import com.mtd.tt.util.LoginUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 经验接口：经验变化记录、等级挡位关系。当前经验与等级随用户资料返回（userProfile.exp / level）。
 */
@Tag(name = "经验", description = "经验变化记录与等级挡位")
@RestController
@RequestMapping("/exp")
@RequiredArgsConstructor
public class ExpController {

    private final ExpService expService;

    @Operation(summary = "查询当前用户经验记录（分页）")
    @PostMapping("/records")
    public Result<PageResponse<ExpRecordResp>> records(@RequestBody(required = false) PageRequest page) {
        Long userId = requireLogin();
        PageRequest p = page == null ? new PageRequest() : page;
        return Result.success(expService.records(userId, p));
    }

    @Operation(summary = "经验与等级挡位关系")
    @PostMapping("/rules")
    public Result<List<Map<String, Object>>> rules() {
        return Result.success(expService.levelRules());
    }

    private Long requireLogin() {
        Long userId = LoginUtil.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        return userId;
    }
}
