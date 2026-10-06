package com.mtd.tt.service;

import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import jakarta.annotation.PostConstruct;
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

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * 文件存储服务：基于 AWS SDK v2（S3 标准实现）与 MinIO（S3 兼容）交互。
 */
@Slf4j
@Service
public class FileStorageService {

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

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
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
    }

    /**
     * 后端直传：读取文件字节，PUT 到对象存储，返回可访问 URL。
     */
    public String upload(String objectKey, InputStream stream, long size, String contentType) {
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
     */
    public String presignUploadUrl(String objectKey, String contentType) {
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
     * 删除对象（供测试清理等使用；不对外暴露接口）
     */
    public boolean delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
            return true;
        } catch (Exception e) {
            log.error("删除对象失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private String buildUrl(String objectKey) {
        return buildAccessUrl(objectKey);
    }

    /**
     * 由对象 key 生成可访问 URL（供文件记录等使用）。
     */
    public String buildAccessUrl(String objectKey) {
        return endpoint.replaceAll("/+$", "") + "/" + bucket + "/" + objectKey;
    }
}
