package com.mtd.iam.tmp;

import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Supabase JWT 验证测试 - 方案 2
 * 参考前端 2.html 实现，使用 RAW→DER 转换
 */
@Slf4j
public class SupabaseJwtTest2 {

    private static final String JWKS_URL = "https://hlhehldribarkqbwwccz.supabase.co/auth/v1/.well-known/jwks.json";

    public static void main(String[] args) {
        String testToken = "eyJhbGciOiJFUzI1NiIsImtpZCI6ImVmN2Y0MmIwLTU1NTQtNGZlYi1hZWQxLTE1NTM0Y2M0YjYwOCIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2hsaGVobGRyaWJhcmtxYnd3Y2N6LnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiI2MDYzODAwMi0zNWE2LTQzZTctYTZiMy1hNjFlYmU0MzdmMGMiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzc0NTE4Njk2LCJpYXQiOjE3NzQ1MTUwOTYsImVtYWlsIjoiMzMzOTY5NDA0M0BxcS5jb20iLCJwaG9uZSI6IiIsImFwcF9tZXRhZGF0YSI6eyJwcm92aWRlciI6ImdpdGh1YiIsInByb3ZpZGVycyI6WyJnaXRodWIiXX0sInVzZXJfbWV0YWRhdGEiOnsiYXZhdGFyX3VybCI6Imh0dHBzOi8vYXZhdGFycy5naXRodWJ1c2VyY29udGVudC5jb20vdS8yNjgxMTk4Mz92PTQiLCJlbWFpbCI6IjMzMzk2OTQwNDNAcXEuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8vYXBpLmdpdGh1Yi5jb20iLCJwaG9uZV92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6Inl1YW5udW94aW4iLCJwcm92aWRlcl9pZCI6IjI2ODExOTgzIiwic3ViIjoiMjY4MTE5ODMiLCJ1c2VyX25hbWUiOiJ5dWFubnVveGluIn0sInJvbGUiOiJhdXRoZW50aWNhdGVkIiwiYWFsIjoiYWFsMSIsImFtciI6W3sibWV0aG9kIjoib2F1dGgiLCJ0aW1lc3RhbXAiOjE3NzQ1MTUwOTZ9XSwic2Vzc2lvbl9pZCI6IjkxZjlkMWJhLTNiYzQtNDVmMi1hNDFmLWMyOGE1ZmIxNjExMSIsImlzX2Fub255bW91cyI6ZmFsc2V9._Fg3ixiFkw33eGIe1cIO-Uv1RM9OhuJlvGjZ-Vslwux4Y9UqZC69qdHC8OuXkiLT2ONob6ujFSINivFZLier4g";
        verify(testToken);
    }

    public static void verify(String token) {
        try {
            // 1. 拆分 Token
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.out.println("❌ Token 格式不正确");
                return;
            }
            String headerPayload = parts[0] + "." + parts[1];
            String signature = parts[2];

            // 2. 获取 JWKS（Hutool 发送请求）
            String jwksStr = HttpUtil.get(JWKS_URL);
            JSONObject jwks = JSONUtil.parseObj(jwksStr);
            JSONObject key = jwks.getJSONArray("keys").getJSONObject(0);

            String x = key.getStr("x");
            String y = key.getStr("y");
            String kid = key.getStr("kid");

            // 3. 生成 P-256 公钥
            PublicKey publicKey = generateECPublicKey(x, y);
            
            // 输出两种格式的公钥
            String x509PublicKey = Base64.encode(publicKey.getEncoded());
            String rawPublicKey = getRawPublicKeyBase64(publicKey);
            
            System.out.println("=== 公钥信息 ===");
            System.out.println("kid: " + kid);
            System.out.println("x: " + x);
            System.out.println("y: " + y);
            System.out.println("完整公钥 (X.509/Base64): " + x509PublicKey);
            System.out.println("完整公钥 (Raw/Base64): " + rawPublicKey + " ← 与浏览器一致");
            System.out.println();

            // 4. 验签（关键：JWT 签名是 RAW 格式，需要转为 DER 格式）
            byte[] data = headerPayload.getBytes(StandardCharsets.UTF_8);
            byte[] sigRaw = base64UrlDecode(signature);
            byte[] sigDer = rawToDer(sigRaw);  // 🔥 关键步骤：RAW → DER

            System.out.println("=== 调试信息 ===");
            System.out.println("签名长度 (RAW): " + sigRaw.length + " bytes");
            System.out.println("签名长度 (DER): " + sigDer.length + " bytes");

            Signature signatureVerifier = Signature.getInstance("SHA256withECDSA");
            signatureVerifier.initVerify(publicKey);
            signatureVerifier.update(data);
            boolean valid = signatureVerifier.verify(sigDer);

            // 输出结果
            System.out.println("\n=== 验签结果 ===");
            if (valid) {
                System.out.println("✅ 验签成功！签名真实有效！");
            } else {
                System.out.println("❌ 验签失败！签名无效！");
            }

        } catch (Exception e) {
            System.out.println("❌ 验签异常：" + e.getMessage());
            log.error("验证异常", e);
        }
    }

    // ==================== 生成 P-256 公钥 ====================
    private static PublicKey generateECPublicKey(String x, String y) throws Exception {
        byte[] xBytes = base64UrlDecode(x);
        byte[] yBytes = base64UrlDecode(y);

        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecSpec = parameters.getParameterSpec(ECParameterSpec.class);

        ECPoint point = new ECPoint(
                new BigInteger(1, xBytes),
                new BigInteger(1, yBytes)
        );

        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(new ECPublicKeySpec(point, ecSpec));
    }

    /**
     * 获取 Raw 格式的公钥（0x04 + x + y），与浏览器显示一致
     */
    private static String getRawPublicKeyBase64(PublicKey publicKey) throws Exception {
        java.security.interfaces.ECPublicKey ecPublicKey = 
            (java.security.interfaces.ECPublicKey) publicKey;
        java.math.BigInteger x = ecPublicKey.getW().getAffineX();
        java.math.BigInteger y = ecPublicKey.getW().getAffineY();
        
        byte[] xBytes = x.toByteArray();
        byte[] yBytes = y.toByteArray();
        
        // 确保是 32 字节（去除可能的前导零）
        if (xBytes.length > 32) xBytes = Arrays.copyOfRange(xBytes, xBytes.length - 32, xBytes.length);
        if (yBytes.length > 32) yBytes = Arrays.copyOfRange(yBytes, yBytes.length - 32, yBytes.length);
        
        // 构建 0x04 + x + y
        byte[] raw = new byte[65];
        raw[0] = 0x04;
        System.arraycopy(xBytes, 0, raw, 1 + Math.max(0, 32 - xBytes.length), xBytes.length);
        System.arraycopy(yBytes, 0, raw, 33 + Math.max(0, 32 - yBytes.length), yBytes.length);
        
        return Base64.encode(raw);
    }

    // ==================== Base64URL 解码 ====================
    private static byte[] base64UrlDecode(String str) {
        str = str.replace('-', '+').replace('_', '/');
        while (str.length() % 4 != 0) {
            str += "=";
        }
        return Base64.decode(str);
    }

    // ==================== ES256 签名格式转换 RAW → DER ====================
    private static byte[] rawToDer(byte[] sig) {
        if (sig.length != 64) {
            throw new IllegalArgumentException("Invalid signature length: " + sig.length);
        }

        // 分割 r 和 s（各 32 字节）
        byte[] r = Arrays.copyOfRange(sig, 0, 32);
        byte[] s = Arrays.copyOfRange(sig, 32, 64);

        // 去除前导零
        r = trimLeadingZeros(r);
        s = trimLeadingZeros(s);

        // 如果最高位为 1，需要添加 0x00 填充（表示正数）
        if ((r[0] & 0x80) != 0) r = concat(new byte[]{0}, r);
        if ((s[0] & 0x80) != 0) s = concat(new byte[]{0}, s);

        // 构建 DER 编码：0x30 [总长度] 0x02 [r 长度] [r] 0x02 [s 长度] [s]
        byte[] der = new byte[6 + r.length + s.length];
        der[0] = 0x30;  // SEQUENCE
        der[1] = (byte) (4 + r.length + s.length);
        der[2] = 0x02;  // INTEGER
        der[3] = (byte) r.length;
        System.arraycopy(r, 0, der, 4, r.length);
        der[4 + r.length] = 0x02;  // INTEGER
        der[5 + r.length] = (byte) s.length;
        System.arraycopy(s, 0, der, 6 + r.length, s.length);
        return der;
    }

    private static byte[] trimLeadingZeros(byte[] b) {
        int i = 0;
        while (i < b.length && b[i] == 0) i++;
        return Arrays.copyOfRange(b, i, b.length);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
