package com.mtd.iam.util;

import cn.hutool.core.codec.Base64Decoder;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class PasskeyHelper {

    // 生成随机挑战值
    public static byte[] generateChallenge() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    // SHA256
    public static byte[] sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    // ✅【终极正确】直接解析 X.509 格式公钥（浏览器返回的就是这个）
    public static PublicKey parsePublicKey(byte[] x509PublicKey) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(new X509EncodedKeySpec(x509PublicKey));
    }

    // ✅【终极正确】验签
    public static boolean verifySignature(
            String publicKeyBase64,    // X.509 格式
            String authenticatorDataB64,
            String clientDataJSONB64,
            String signatureB64
    ) throws Exception {

        byte[] authData = Base64Decoder.decode(authenticatorDataB64);
        byte[] clientData = Base64Decoder.decode(clientDataJSONB64);
        byte[] clientHash = sha256(clientData);

        // 拼接：authData + sha256(clientData)
        byte[] data = new byte[authData.length + clientHash.length];
        System.arraycopy(authData, 0, data, 0, authData.length);
        System.arraycopy(clientHash, 0, data, authData.length, clientHash.length);

        // 公钥解析
        PublicKey publicKey = parsePublicKey(Base64Decoder.decode(publicKeyBase64));

        // 验签
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(Base64Decoder.decode(signatureB64));
    }
}