package com.mtd.tt.service;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 文件存储服务：支持两种模式，运行期可由 sys_config 的 file.storage.type 切换。
 * <ul>
 *   <li>minio：AWS SDK v2（S3 标准实现）与 MinIO（S3 兼容）交互</li>
 *   <li>local：写入本机磁盘，通过 /files/** 静态映射对外提供访问</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final SysConfigService sysConfigService;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.region:cn-east-1}")
    private String region;

    @Value("${minio.presign-expiry-seconds:3600}")
    private int presignExpiry;

    /** 默认存储模式（sys_config 未配置 file.storage.type 时使用） */
    @Value("${file.storage.type:minio}")
    private String defaultStorageType;

    /** 本地存储根目录 */
    @Value("${file.local.root:./data/files}")
    private String localRoot;

    /** 本地存储访问前缀，留空则返回相对路径 */
    @Value("${file.local.base-url:}")
    private String localBaseUrl;

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private Path localDir;

    @PostConstruct
    public void init() {
        this.localDir = Paths.get(localRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(localDir);
        } catch (IOException e) {
            log.warn("本地存储目录创建失败: {} ({})", localDir, e.getMessage());
        }
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        URI endpointUri = URI.create(endpoint);
        StaticCredentialsProvider provider = StaticCredentialsProvider.create(credentials);
        this.s3Client = S3Client.builder()
                .endpointOverride(endpointUri)
                .region(Region.of(region))
                .credentialsProvider(provider)
                .forcePathStyle(true)
                .build();
        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(endpointUri)
                .region(Region.of(region))
                .credentialsProvider(provider)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        log.info("文件存储模式: {}，本地目录: {}", currentType(), localDir);
    }

    /**
     * 当前存储模式：sys_config 的 file.storage.type 优先，缺省回落到 yml 配置。
     */
    public String currentType() {
        String v = null;
        try {
            v = sysConfigService.get("file.storage.type");
        } catch (Exception ignored) {
            // 配置服务尚未就绪时按 yml 默认走
        }
        if (v == null || v.isBlank()) {
            v = defaultStorageType;
        }
        return v.trim().toLowerCase();
    }

    public boolean isLocal() {
        return "local".equals(currentType());
    }

    /**
     * 后端直传：写入对象存储或本地磁盘，返回可访问 URL。
     */
    public String upload(String objectKey, InputStream stream, long size, String contentType) {
        if (isLocal()) {
            return uploadLocal(objectKey, stream);
        }
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey);
            if (contentType != null && !contentType.isBlank()) {
                builder.contentType(contentType);
            }
            s3Client.putObject(builder.build(), RequestBody.fromInputStream(stream, size));
            return buildUrl(objectKey);
        } catch (Exception e) {
            log.error("上传文件到对象存储失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 预签名直传：返回一个限时 PUT 上传地址（S3 标准预签名 URL），前端直接 PUT。
     * 本地磁盘模式无法直传，调用方应改用 /file/upload。
     */
    public String presignUploadUrl(String objectKey, String contentType) {
        if (isLocal()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "本地存储模式不支持预签名直传，请改用 /file/upload 上传");
        }
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey);
            if (contentType != null && !contentType.isBlank()) {
                builder.contentType(contentType);
            }
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofSeconds(presignExpiry))
                            .putObjectRequest(builder.build())
                            .build());
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("生成预签名上传地址失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "生成上传地址失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件（供测试清理等使用；不对外暴露接口）
     */
    public boolean delete(String objectKey) {
        boolean ok = true;
        // 兼容历史本地文件：即使已切到对象存储，也尝试清理同 key 的旧磁盘文件
        try {
            Files.deleteIfExists(resolveLocalPath(objectKey));
        } catch (Exception e) {
            // 本地不存在或路径不合法都不影响对象存储删除
            log.debug("清理本地历史文件跳过: {}", e.getMessage());
        }
        if (!isLocal()) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
            } catch (Exception e) {
                log.error("删除对象失败: {}", e.getMessage(), e);
                ok = false;
            }
        }
        return ok;
    }

    private String uploadLocal(String objectKey, InputStream stream) {
        Path target = resolveLocalPath(objectKey);
        try (InputStream in = stream) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("写入本地文件失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "文件保存失败: " + e.getMessage());
        }
        return buildAccessUrl(objectKey);
    }

    /**
     * 把对象 key 解析成磁盘路径，并校验不会越出存储根目录。
     */
    private Path resolveLocalPath(String objectKey) {
        Path target = localDir.resolve(objectKey).normalize();
        if (!target.startsWith(localDir)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "非法的文件路径");
        }
        return target;
    }

    private String buildUrl(String objectKey) {
        return buildAccessUrl(objectKey);
    }

    /**
     * 由对象 key 生成可访问 URL（供文件记录等使用）。
     */
    public String buildAccessUrl(String objectKey) {
        if (isLocal()) {
            String base = localBaseUrl == null ? "" : localBaseUrl.trim().replaceAll("/+$", "");
            return base + "/files/" + objectKey;
        }
        return endpoint.replaceAll("/+$", "") + "/" + bucket + "/" + objectKey;
    }

    /** 本地存储根目录（供静态资源映射使用） */
    public Path getLocalDir() {
        return localDir;
    }
}
