package com.mtd.tt.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.Result;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.enums.FileSceneEnum;
import com.mtd.tt.mapper.UserFileMapper;
import com.mtd.tt.model.entity.UserFileEntity;
import com.mtd.tt.model.req.PresignUploadReq;
import com.mtd.tt.model.resp.FileUploadResp;
import com.mtd.tt.service.FileStorageService;
import com.mtd.tt.service.PointService;
import com.mtd.tt.util.LoginUtil;
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
import java.time.OffsetDateTime;

/**
 * 文件接口：后端上传 / 预签名直传。
 * 上传按「场景(scene) / 年月(yyyyMM) / 用户id + uuid」组织目录，
 * 缩略图在文件名中以 _thumb 体现；每次上传记录到 user_files 表，发推后可回填关联推文。
 */
@Tag(name = "文件", description = "文件上传（MinIO）")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final UserFileMapper userFileMapper;
    private final PointService pointService;

    @Operation(summary = "上传文件（multipart/form-data，字段名 file；scene=avatar/banner/post，thumbnail=true 表示缩略图）")
    @PostMapping("/upload")
    public Result<FileUploadResp> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "scene", required = false, defaultValue = "post") String scene,
            @RequestParam(value = "thumbnail", required = false, defaultValue = "false") boolean thumbnail,
            @RequestParam(value = "mediaType", required = false) String mediaType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上传文件不能为空");
        }
        Long userId = requireLogin();
        FileSceneEnum sceneEnum = resolveScene(scene);
        // 上传扣积分（按文件大小，不足1MB按1MB）；积分不足拒绝
        pointService.deductForUpload(userId, file.getSize());
        String objectKey = buildObjectKey(userId, sceneEnum, file.getOriginalFilename(), thumbnail);
        try {
            String url = fileStorageService.upload(objectKey, file.getInputStream(), file.getSize(), file.getContentType());
            UserFileEntity rec = saveFileRecord(userId, sceneEnum, mediaType, objectKey, url, file.getSize(), file.getContentType(), thumbnail);
            return Result.success(toResp(url, rec.getId(), sceneEnum.getCode()));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.ERROR.getCode(), "读取上传文件失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取预签名上传地址（返回 PUT 直传 URL；scene 指定目录，thumbnail=true 表示缩略图）")
    @PostMapping("/presignUpload")
    public Result<FileUploadResp> presignUpload(@RequestBody PresignUploadReq req) {
        if (StrUtil.isBlank(req.getFileName())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "fileName 不能为空");
        }
        Long userId = requireLogin();
        FileSceneEnum sceneEnum = resolveScene(req.getScene());
        boolean thumbnail = Boolean.TRUE.equals(req.getThumbnail());
        // 上传扣积分（按前端上报的文件大小，不足1MB按1MB）；积分不足拒绝
        long size = req.getSize() == null ? 0L : req.getSize();
        pointService.deductForUpload(userId, size);
        String objectKey = buildObjectKey(userId, sceneEnum, req.getFileName(), thumbnail);
        String presignUrl = fileStorageService.presignUploadUrl(objectKey, req.getContentType());
        // 预建文件记录：url 存可访问地址（供发推时按 url 关联），前端 PUT 成功后即实际生效
        UserFileEntity rec = saveFileRecord(userId, sceneEnum, null, objectKey,
                fileStorageService.buildAccessUrl(objectKey), null, req.getContentType(), thumbnail);
        return Result.success(toResp(presignUrl, rec.getId(), sceneEnum.getCode()));
    }

    private FileUploadResp toResp(String url, Long fileId, String scene) {
        FileUploadResp resp = new FileUploadResp();
        resp.setUrl(url);
        resp.setFileId(fileId);
        resp.setScene(scene);
        return resp;
    }

    private Long requireLogin() {
        Long userId = LoginUtil.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "请先登录");
        }
        return userId;
    }

    private FileSceneEnum resolveScene(String scene) {
        FileSceneEnum sceneEnum = FileSceneEnum.of(scene);
        if (sceneEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "不支持的场景: " + scene + "（可选: " + FileSceneEnum.OPTIONS + "）");
        }
        return sceneEnum;
    }

    /**
     * 对象 key：{scene}/{yyyyMM}/u_{userId}_{uuid}{_thumb}.{ext}
     */
    private String buildObjectKey(Long userId, FileSceneEnum scene, String fileName, boolean thumbnail) {
        String ext = "";
        if (StrUtil.isNotBlank(fileName) && fileName.contains(".")) {
            String raw = fileName.substring(fileName.lastIndexOf(".") + 1);
            String cleaned = raw.replaceAll("[^a-zA-Z0-9]", "");
            if (!cleaned.isBlank() && cleaned.length() <= 12) {
                ext = "." + cleaned;
            }
        }
        String month = DateUtil.format(DateUtil.date(), "yyyyMM");
        String thumb = thumbnail ? "_thumb" : "";
        return scene.getCode() + "/" + month + "/u_" + userId + "_" + IdUtil.fastSimpleUUID() + thumb + ext;
    }

    private UserFileEntity saveFileRecord(Long userId, FileSceneEnum scene, String mediaType,
                                          String objectKey, String url, Long size, String contentType, boolean thumbnail) {
        UserFileEntity rec = new UserFileEntity();
        rec.setUserId(userId);
        rec.setScene(scene.getCode());
        rec.setMediaType(mediaType);
        rec.setObjectKey(objectKey);
        rec.setUrl(url);
        rec.setSize(size);
        rec.setContentType(contentType);
        rec.setThumbnail(thumbnail);
        rec.setCreatedAt(OffsetDateTime.now());
        userFileMapper.insertSelective(rec);
        return rec;
    }
}
