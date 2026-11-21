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
import com.mtd.tt.model.req.DeleteFileReq;
import com.mtd.tt.model.req.PresignUploadReq;
import com.mtd.tt.model.resp.FileUploadResp;
import com.mtd.tt.model.resp.MyFileItemResp;
import com.mtd.tt.service.FileStorageService;
import com.mtd.tt.service.PointService;
import com.mtd.tt.util.LoginUtil;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
            @RequestParam(value = "thumbnailOfFileId", required = false) Long thumbnailOfFileId,
            @RequestParam(value = "mediaType", required = false) String mediaType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "上传文件不能为空");
        }
        Long userId = requireLogin();
        FileSceneEnum sceneEnum = resolveScene(scene);
        Long parentId = resolveThumbnailParent(userId, thumbnail, thumbnailOfFileId);
        // 上传扣积分（按文件大小，不足1MB按1MB）；积分不足拒绝
        pointService.deductForUpload(userId, file.getSize());
        String objectKey = buildObjectKey(userId, sceneEnum, file.getOriginalFilename(), thumbnail);
        try {
            String url = fileStorageService.upload(objectKey, file.getInputStream(), file.getSize(), file.getContentType());
            UserFileEntity rec = saveFileRecord(userId, sceneEnum, mediaType, objectKey, url, file.getSize(), file.getContentType(), thumbnail, parentId);
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
        Long parentId = resolveThumbnailParent(userId, thumbnail, req.getThumbnailOfFileId());
        // 上传扣积分（按前端上报的文件大小，不足1MB按1MB）；积分不足拒绝
        long size = req.getSize() == null ? 0L : req.getSize();
        pointService.deductForUpload(userId, size);
        String objectKey = buildObjectKey(userId, sceneEnum, req.getFileName(), thumbnail);
        String presignUrl = fileStorageService.presignUploadUrl(objectKey, req.getContentType());
        // 预建文件记录：url 存可访问地址（供发推时按 url 关联），前端 PUT 成功后即实际生效
        UserFileEntity rec = saveFileRecord(userId, sceneEnum, null, objectKey,
                fileStorageService.buildAccessUrl(objectKey), null, req.getContentType(), thumbnail, parentId);
        return Result.success(toResp(presignUrl, rec.getId(), sceneEnum.getCode()));
    }

    @Operation(summary = "当前用户上传过的媒体列表（文章编辑器「我的图片 / 我的视频」）")
    @GetMapping("/myFiles")
    public Result<Map<String, List<MyFileItemResp>>> myFiles(
            @RequestParam(value = "mediaType", required = false) String mediaType,
            @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit) {
        Long userId = requireLogin();
        int max = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(UserFileEntity::getUserId, userId)
                .eq(UserFileEntity::getScene, "post");
        wrapper.orderBy(UserFileEntity::getCreatedAt, false).orderBy(UserFileEntity::getId, false);
        List<UserFileEntity> all = userFileMapper.selectListByQuery(wrapper);
        // 缩略图与原图是两条独立上传记录（各自 uuid）。新数据在缩略图行上直接保存原图 id，
        // 老数据没有该字段，仍按「同一推文 + 创建时间紧随原图」兜底。
        List<UserFileEntity> thumbs = all.stream()
                .filter(f -> Boolean.TRUE.equals(f.getThumbnail()))
                .toList();
        java.util.Map<Long, UserFileEntity> explicitThumb = new java.util.HashMap<>();
        for (UserFileEntity t : thumbs) {
            if (t.getThumbnailOfFileId() != null) {
                explicitThumb.put(t.getThumbnailOfFileId(), t);
            }
        }
        java.util.Set<Long> usedThumb = new java.util.HashSet<>();
        String wantedType = StrUtil.isBlank(mediaType) ? null : mediaType.trim();
        List<MyFileItemResp> items = new java.util.ArrayList<>();
        for (UserFileEntity f : all) {
            if (Boolean.TRUE.equals(f.getThumbnail())) {
                continue;
            }
            if (wantedType != null && !wantedType.equals(f.getMediaType())) {
                continue;
            }
            if (items.size() >= max) {
                break;
            }
            UserFileEntity best = null;
            long bestScore = Long.MAX_VALUE;
            for (UserFileEntity t : thumbs) {
                if (usedThumb.contains(t.getId()) || f.getCreatedAt() == null || t.getCreatedAt() == null
                        || t.getCreatedAt().isBefore(f.getCreatedAt())) {
                    continue;
                }
                long delta = java.time.Duration.between(f.getCreatedAt(), t.getCreatedAt()).toMillis();
                if (delta > 15_000L) {
                    continue;
                }
                // 未发过帖的原图只配未绑定的缩略图；已发过帖的原图优先同推文的缩略图。
                if (f.getPostId() == null) {
                    if (t.getPostId() != null) {
                        continue;
                    }
                } else if (t.getPostId() != null && !t.getPostId().equals(f.getPostId())) {
                    continue;
                }
                long score = delta;
                if (f.getPostId() != null) {
                    score = t.getPostId() == null ? delta + (1L << 50) : delta - (1L << 40);
                }
                if (score < bestScore) {
                    bestScore = score;
                    best = t;
                }
            }
            // 显式关联优先：缩略图行直接记录了原图 id，不受 15 秒时间窗限制
            UserFileEntity explicit = explicitThumb.get(f.getId());
            if (explicit != null && !usedThumb.contains(explicit.getId())) {
                best = explicit;
            }
            MyFileItemResp item = new MyFileItemResp();
            item.setId(f.getId());
            item.setUrl(f.getUrl());
            item.setMediaType(f.getMediaType());
            item.setContentType(f.getContentType());
            item.setSize(f.getSize());
            item.setPostId(f.getPostId());
            item.setCreatedAt(f.getCreatedAt());
            if (best != null) {
                usedThumb.add(best.getId());
                item.setThumbnailUrl(best.getUrl());
                // 缩略图已归属旧推文时只复用地址，不返回 id，避免新帖把它从原帖解绑。
                if (best.getPostId() == null) {
                    item.setThumbnailFileId(best.getId());
                }
            }
            items.add(item);
        }
        return Result.success(Map.of("items", items));
    }

    @Operation(summary = "删除当前用户上传且未被推文使用的媒体文件")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody DeleteFileReq req) {
        if (req.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件 id 不能为空");
        }
        Long userId = requireLogin();
        UserFileEntity file = userFileMapper.selectOneById(req.getId());
        if (file == null || !userId.equals(file.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文件不存在或无权删除");
        }
        if (file.getPostId() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该文件已被推文使用，不能删除");
        }
        if (file.getObjectKey() == null || file.getObjectKey().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件记录缺少对象路径，无法删除");
        }
        if (!fileStorageService.delete(file.getObjectKey())) {
            throw new BusinessException(ResultCode.ERROR.getCode(), "文件删除失败，请稍后重试");
        }
        userFileMapper.deleteById(file.getId());
        return Result.success();
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

    /**
     * 校验缩略图显式关联的原图 id：必须存在、属于当前用户，且本身不是缩略图。
     */
    private Long resolveThumbnailParent(Long userId, boolean thumbnail, Long thumbnailOfFileId) {
        if (thumbnailOfFileId == null) {
            return null;
        }
        if (!thumbnail) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "只有缩略图才能指定 thumbnailOfFileId");
        }
        UserFileEntity parent = userFileMapper.selectOneByQuery(QueryWrapper.create()
                .eq(UserFileEntity::getId, thumbnailOfFileId)
                .eq(UserFileEntity::getUserId, userId));
        if (parent == null || Boolean.TRUE.equals(parent.getThumbnail())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "缩略图关联的原图不存在或不属于当前用户");
        }
        return thumbnailOfFileId;
    }

    private UserFileEntity saveFileRecord(Long userId, FileSceneEnum scene, String mediaType,
                                          String objectKey, String url, Long size, String contentType, boolean thumbnail,
                                          Long thumbnailOfFileId) {
        UserFileEntity rec = new UserFileEntity();
        rec.setUserId(userId);
        rec.setScene(scene.getCode());
        rec.setMediaType(mediaType);
        rec.setObjectKey(objectKey);
        rec.setUrl(url);
        rec.setSize(size);
        rec.setContentType(contentType);
        rec.setThumbnail(thumbnail);
        rec.setThumbnailOfFileId(thumbnailOfFileId);
        rec.setCreatedAt(OffsetDateTime.now());
        userFileMapper.insertSelective(rec);
        return rec;
    }
}
