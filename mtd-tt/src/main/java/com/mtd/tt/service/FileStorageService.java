package com.mtd.tt.service;

import cn.hutool.core.util.ObjUtil;
import com.mtd.common.core.exception.BusinessException;
import com.mtd.common.core.result.ResultCode;
import com.mtd.tt.util.AwsV4Signer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件存储服务：通过 S3 兼容协议（AWS4 签名）与 MinIO 交互，纯 JDK 实现，零第三方依赖。
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

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 后端直传：读取文件字节，AWS4 签名后 PUT 到 MinIO，返回可访问 URL。
     */
    public String upload(String objectKey, InputStream stream, long size, String contentType) {
        try {
            byte[] body = stream.readNBytes((int) size);
            String host = hostOf(endpoint);
            String path = "/" + bucket + "/" + objectKey;
            Map<String, String> extra = new HashMap<>();
            if (contentType != null && !contentType.isBlank()) {
                extra.put("content-type", contentType);
            }
            Map<String, String> headers = AwsV4Signer.signRequest(
                    "PUT", accessKey, secretKey, region, AwsV4Signer.SERVICE_S3, host, path, extra, body);

            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + path))
                    .timeout(Duration.ofSeconds(120))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (ObjUtil.equal(key, "host")) {
                    continue;
                }
                String value = entry.getValue();
                rb.header(key, value);
            }
            HttpResponse<String> resp = httpClient.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return buildUrl(objectKey);
            }
            log.error("MinIO 上传失败 status={} body={}", resp.statusCode(), resp.body());
            throw new BusinessException(ResultCode.ERROR.getCode(), "文件上传失败: HTTP " + resp.statusCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传文件到 MinIO 失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 预签名直传：返回一个限时 PUT 上传地址（AWS4 预签名 URL），前端直接 PUT（参考 S3 直传模式）。
     */
    public String presignUploadUrl(String objectKey, String contentType) {
        try {
            String host = hostOf(endpoint);
            String path = "/" + bucket + "/" + objectKey;
            return AwsV4Signer.presignPutUrl(accessKey, secretKey, region, AwsV4Signer.SERVICE_S3,
                    host, path, contentType, presignExpiry, endpoint);
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
            String host = hostOf(endpoint);
            String path = "/" + bucket + "/" + objectKey;
            Map<String, String> headers = AwsV4Signer.signRequest(
                    "DELETE", accessKey, secretKey, region, AwsV4Signer.SERVICE_S3, host, path, null, new byte[0]);
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + path))
                    .timeout(Duration.ofSeconds(30))
                    .DELETE();
            // host 由 HttpClient 自动设置，手动设置会被拒绝；签名中的 host 与实际一致即可
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!ObjUtil.equal(entry.getKey(), "host")) {
                    rb.header(entry.getKey(), entry.getValue());
                }
            }
            HttpResponse<String> resp = httpClient.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (Exception e) {
            log.error("删除 MinIO 对象失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private String hostOf(String endpoint) {
        return endpoint.replaceAll("^https?://", "").replaceAll("/.*$", "");
    }

    private String buildUrl(String objectKey) {
        return endpoint.replaceAll("/+$", "") + "/" + bucket + "/" + objectKey;
    }
}
