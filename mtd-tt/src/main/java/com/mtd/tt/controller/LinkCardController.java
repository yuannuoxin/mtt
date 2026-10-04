package com.mtd.tt.controller;

import cn.hutool.core.util.StrUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.model.entity.PostLinkCardEntity;
import com.mtd.tt.model.req.ParseLinkCardReq;
import com.mtd.tt.model.resp.PostLinkCardResp;
import com.mtd.tt.service.LinkCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 链接卡片接口：发推时解析外部链接生成卡片预览
 */
@Tag(name = "链接卡片", description = "解析外部链接生成卡片（预览，不落库）")
@RestController
@RequestMapping("/linkCard")
@RequiredArgsConstructor
public class LinkCardController {

    private final LinkCardService linkCardService;

    @Operation(summary = "解析链接生成卡片预览（供前端发推时展示）")
    @PostMapping("/parse")
    public Result<PostLinkCardResp> parse(@RequestBody ParseLinkCardReq req) {
        if (StrUtil.isBlank(req.getUrl())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "链接不能为空");
        }
        PostLinkCardEntity c = linkCardService.resolve(req.getUrl().trim());
        PostLinkCardResp r = new PostLinkCardResp();
        r.setCardType(c.getCardType());
        r.setTargetUrl(c.getTargetUrl());
        r.setTitle(c.getTitle());
        r.setDescription(c.getDescription());
        r.setVideoProvider(c.getVideoProvider());
        r.setVideoId(c.getVideoId());
        r.setCoverMediaUrl(c.getCoverMediaUrl());
        return Result.success(r);
    }
}
