package com.mtd.iam.tmp;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;

public class SupabaseJwtTest {

    // ====================== 配置 ======================
    private static final String SUPABASE_URL = "https://hlhehldribarkqbwwccz.supabase.co";
    private static final String JWKS_URL = SUPABASE_URL + "/auth/v1/.well-known/jwks.json";

    public static void main(String[] args) {
        // 在这里放入你的 Token
        String token = "eyJhbGciOiJFUzI1NiIsImtpZCI6ImVmN2Y0MmIwLTU1NTQtNGZlYi1hZWQxLTE1NTM0Y2M0YjYwOCIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2hsaGVobGRyaWJhcmtxYnd3Y2N6LnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiI2MDYzODAwMi0zNWE2LTQzZTctYTZiMy1hNjFlYmU0MzdmMGMiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzc0NTE4Njk2LCJpYXQiOjE3NzQ1MTUwOTYsImVtYWlsIjoiMzMzOTY5NDA0M0BxcS5jb20iLCJwaG9uZSI6IiIsImFwcF9tZXRhZGF0YSI6eyJwcm92aWRlciI6ImdpdGh1YiIsInByb3ZpZGVycyI6WyJnaXRodWIiXX0sInVzZXJfbWV0YWRhdGEiOnsiYXZhdGFyX3VybCI6Imh0dHBzOi8vYXZhdGFycy5naXRodWJ1c2VyY29udGVudC5jb20vdS8yNjgxMTk4Mz92PTQiLCJlbWFpbCI6IjMzMzk2OTQwNDNAcXEuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8vYXBpLmdpdGh1Yi5jb20iLCJwaG9uZV92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6Inl1YW5udW94aW4iLCJwcm92aWRlcl9pZCI6IjI2ODExOTgzIiwic3ViIjoiMjY4MTE5ODMiLCJ1c2VyX25hbWUiOiJ5dWFubnVveGluIn0sInJvbGUiOiJhdXRoZW50aWNhdGVkIiwiYWFsIjoiYWFsMSIsImFtciI6W3sibWV0aG9kIjoib2F1dGgiLCJ0aW1lc3RhbXAiOjE3NzQ1MTUwOTZ9XSwic2Vzc2lvbl9pZCI6IjkxZjlkMWJhLTNiYzQtNDVmMi1hNDFmLWMyOGE1ZmIxNjExMSIsImlzX2Fub255bW91cyI6ZmFsc2V9._Fg3ixiFkw33eGIe1cIO-Uv1RM9OhuJlvGjZ-Vslwux4Y9UqZC69qdHC8OuXkiLT2ONob6ujFSINivFZLier4g";
        verify(token);
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
            String fullPublicKey = Base64.encode(publicKey.getEncoded());

            // 输出公钥信息
            System.out.println("=== 公钥信息 ===");
            System.out.println("kid: " + kid);
            System.out.println("x: " + x);
            System.out.println("y: " + y);
            System.out.println("完整公钥(Base64): " + fullPublicKey);
            System.out.println();

            // 4. 验签
            byte[] data = headerPayload.getBytes(StandardCharsets.UTF_8);
            byte[] sigRaw = base64UrlDecode(signature);
            byte[] sigDer = rawToDer(sigRaw);

            Signature signatureVerifier = Signature.getInstance("SHA256withECDSA");
            signatureVerifier.initVerify(publicKey);
            signatureVerifier.update(data);
            boolean valid = signatureVerifier.verify(sigDer);

            // 输出结果
            System.out.println("=== 验签结果 ===");
            if (valid) {
                System.out.println("✅ 验签成功！签名真实有效！");
            } else {
                System.out.println("❌ 验签失败！签名无效！");
            }

        } catch (Exception e) {
            System.out.println("❌ 验签异常：" + e.getMessage());
            e.printStackTrace();
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
                new java.math.BigInteger(1, xBytes),
                new java.math.BigInteger(1, yBytes)
        );

        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(new ECPublicKeySpec(point, ecSpec));
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
        byte[] r = Arrays.copyOfRange(sig, 0, 32);
        byte[] s = Arrays.copyOfRange(sig, 32, 64);

        r = trimLeadingZeros(r);
        s = trimLeadingZeros(s);

        if ((r[0] & 0x80) != 0) r = concat(new byte[]{0}, r);
        if ((s[0] & 0x80) != 0) s = concat(new byte[]{0}, s);

        byte[] der = new byte[6 + r.length + s.length];
        der[0] = 0x30;
        der[1] = (byte) (4 + r.length + s.length);
        der[2] = 0x02;
        der[3] = (byte) r.length;
        System.arraycopy(r, 0, der, 4, r.length);
        der[4 + r.length] = 0x02;
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