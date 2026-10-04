package com.mtd.tt.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * AWS Signature Version 4 签名工具（纯 JDK 实现，零第三方依赖）。
 * 用于与 S3 兼容对象存储（MinIO 等）交互：PUT 上传签名、DELETE 签名、预签名 URL。
 */
public final class AwsV4Signer {

    public static final String SERVICE_S3 = "s3";

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String TERMINATOR = "aws4_request";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    private AwsV4Signer() {
    }

    /**
     * 为一次带 body 的请求（PUT 上传等）计算完整签名，返回最终应附加到请求上的所有头（含 Authorization）。
     *
     * @param method       HTTP 方法（PUT/DELETE/GET 等）
     * @param accessKey    AccessKey
     * @param secretKey    SecretKey
     * @param region       区域（如 cn-east-1）
     * @param service      服务名（S3 固定 s3）
     * @param host         请求 Host（如 103.117.139.151:9000）
     * @param path         URI 路径（如 /001/a.png）
     * @param extraHeaders 需参与签名的额外头（如 content-type），key 不区分大小写
     * @param payload      请求体（GET/DELETE 无体可传空数组）
     */
    public static Map<String, String> signRequest(String method, String accessKey, String secretKey,
                                                  String region, String service, String host, String path,
                                                  Map<String, String> extraHeaders, byte[] payload) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = amzDate(now);
        String dateStamp = dateStamp(now);
        String payloadHash = sha256Hex(payload);

        Map<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-content-sha256", payloadHash);
        if (extraHeaders != null) {
            for (Map.Entry<String, String> e : extraHeaders.entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }
                headers.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().trim());
            }
        }

        String canonicalHeaders = buildCanonicalHeaders(headers);
        String signedHeaders = String.join(";", headers.keySet());
        String canonicalRequest = method + "\n"
                + uriEncode(path, false) + "\n"
                + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;

        String scope = dateStamp + "/" + region + "/" + service + "/" + TERMINATOR;
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + scope + "\n" + sha256Hex(canonicalRequest);

        byte[] signingKey = deriveKey(secretKey, dateStamp, region, service);
        String signature = hex(hmac(signingKey, stringToSign.getBytes(StandardCharsets.UTF_8)));

        Map<String, String> result = new TreeMap<>(headers);
        result.put("authorization", ALGORITHM + " Credential=" + accessKey + "/" + scope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature);
        return result;
    }

    /**
     * 生成 S3 兼容的预签名 PUT 上传 URL（前端直传模式）。
     */
    public static String presignPutUrl(String accessKey, String secretKey, String region,
                                       String service, String host, String path, String contentType,
                                       long expiresSeconds, String endpoint) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = amzDate(now);
        String dateStamp = dateStamp(now);
        String scope = dateStamp + "/" + region + "/" + service + "/" + TERMINATOR;

        Map<String, String> signedHeaderValues = new TreeMap<>();
        signedHeaderValues.put("host", host);
        if (contentType != null && !contentType.isBlank()) {
            signedHeaderValues.put("content-type", contentType.trim());
        }
        // signed headers 必须按字典序，与 canonical headers 顺序一致
        String signedHeaders = String.join(";", signedHeaderValues.keySet());

        TreeMap<String, String> query = new TreeMap<>();
        query.put("X-Amz-Algorithm", ALGORITHM);
        query.put("X-Amz-Credential", accessKey + "/" + scope);
        query.put("X-Amz-Date", amzDate);
        query.put("X-Amz-Expires", String.valueOf(expiresSeconds));
        query.put("X-Amz-SignedHeaders", signedHeaders);

        String canonicalQuery = canonicalQueryString(query);
        String canonicalHeaders = buildCanonicalHeaders(signedHeaderValues);
        String canonicalRequest = "PUT\n"
                + uriEncode(path, false) + "\n"
                + canonicalQuery + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + UNSIGNED_PAYLOAD;

        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + scope + "\n" + sha256Hex(canonicalRequest);
        byte[] signingKey = deriveKey(secretKey, dateStamp, region, service);
        String signature = hex(hmac(signingKey, stringToSign.getBytes(StandardCharsets.UTF_8)));

        query.put("X-Amz-Signature", signature);
        return endpoint.replaceAll("/+$", "") + uriEncode(path, false) + "?" + canonicalQueryString(query);
    }

    private static byte[] deriveKey(String secretKey, String dateStamp, String region, String service) {
        byte[] kDate = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp.getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmac(kDate, region.getBytes(StandardCharsets.UTF_8));
        byte[] kService = hmac(kRegion, service.getBytes(StandardCharsets.UTF_8));
        return hmac(kService, TERMINATOR.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 不可用", e);
        }
    }

    public static String sha256Hex(byte[] data) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    public static String sha256Hex(String text) {
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String buildCanonicalHeaders(Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            sb.append(e.getKey()).append(':').append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    private static String canonicalQueryString(TreeMap<String, String> query) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : query.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(queryEncode(e.getKey(), true)).append('=').append(queryEncode(e.getValue(), true));
        }
        return sb.toString();
    }

    /**
     * 查询串编码（AWS SigV4，RFC3986）：仅保留 unreserved 字符；'/' 编码为 %2F，分号编码为 %3B。
     * （MinIO 重建 canonical query 时按标准 URL 编码处理 SignedHeaders 中的分号。）
     */
    private static String queryEncode(String input, boolean encodeSlash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (isUnreserved(c) || (c == '/' && !encodeSlash)) {
                sb.append((char) c);
            } else {
                sb.append('%').append(String.format(Locale.ROOT, "%02X", c));
            }
        }
        return sb.toString();
    }

    static String uriEncode(String input, boolean encodeSlash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (isUnreserved(c) || (c == '/' && !encodeSlash)) {
                sb.append((char) c);
            } else {
                sb.append('%').append(String.format(Locale.ROOT, "%02X", c));
            }
        }
        return sb.toString();
    }

    private static boolean isUnreserved(int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || c == '-' || c == '_' || c == '.' || c == '~';
    }

    private static String amzDate(ZonedDateTime now) {
        return now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }

    private static String dateStamp(ZonedDateTime now) {
        return now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }
}
