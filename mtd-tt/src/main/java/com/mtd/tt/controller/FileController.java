package com.mtd.tt.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.model.req.PresignUploadReq;
import com.mtd.tt.model.resp.FileUploadResp;
import com.mtd.tt.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件接口：后端上传 / 预签名直传
 */
@Tag(name = "文件", description = "文件上传（MinIO）")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传文件（multipart/form-data，字段名 file）")
    @PostMapping("/upload")
    @SaCheckLogin
    public Result<FileUploadResp> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上传文件不能为空");
        }
        String objectKey = buildObjectKey(file.getOriginalFilename());
        try {
            String url = fileStorageService.upload(objectKey, file.getInputStream(), file.getSize(), file.getContentType());
            return Result.success(toResp(url));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.ERROR.getCode(), "读取上传文件失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取预签名上传地址（返回 PUT 直传 URL）")
    @PostMapping("/presignUpload")
    @SaCheckLogin
    public Result<FileUploadResp> presignUpload(@RequestBody PresignUploadReq req) {
        if (StrUtil.isBlank(req.getFileName())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "fileName 不能为空");
        }
        String objectKey = buildObjectKey(req.getFileName());
        return Result.success(toResp(fileStorageService.presignUploadUrl(objectKey, req.getContentType())));
    }

    private FileUploadResp toResp(String url) {
        FileUploadResp resp = new FileUploadResp();
        resp.setUrl(url);
        return resp;
    }

    private String buildObjectKey(String fileName) {
        String ext = "";
        if (StrUtil.isNotBlank(fileName) && fileName.contains(".")) {
            String raw = fileName.substring(fileName.lastIndexOf(".") + 1);
            // 只保留字母数字扩展名，避免路径/特殊字符注入
            String cleaned = raw.replaceAll("[^a-zA-Z0-9]", "");
            if (!cleaned.isBlank() && cleaned.length() <= 12) {
                ext = "." + cleaned;
            }
        }
        return IdUtil.fastSimpleUUID() + ext;
    }
}
