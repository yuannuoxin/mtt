package com.mtd.iam.tmp;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

public class PasskeyTest {
    /**
     * 1. 模拟生成 Passkey：
     * 生成密钥对 + 模拟 credentialId
     */
    public static Map<String, String> generatePasskey() {
        // 生成 ES256 密钥对（WebAuthn 标准）
        KeyPair keyPair = SecureUtil.generateKeyPair("EC");

        // 公钥（存数据库）
        String publicKey = Base64.encode(keyPair.getPublic().getEncoded());

        // 私钥（模拟设备保存，不入库）
        String privateKey = Base64.encode(keyPair.getPrivate().getEncoded());

        // 模拟浏览器生成的 credentialId（唯一ID）
        String credentialId = Base64.encodeUrlSafe(RandomUtil.randomBytes(16));

        Map<String, String> result = new HashMap<>();
        result.put("credentialId", credentialId);
        result.put("publicKey", publicKey);
        result.put("privateKey", privateKey); // 仅模拟用
        return result;
    }

    /**
     * 2. 模拟设备签名（登录时用私钥签名）
     */
    public static String sign(
            String privateKeyBase64,
            String challenge
    ) {
        byte[] data = challenge.getBytes(StandardCharsets.UTF_8);

        Sign sign = SecureUtil.sign(
                SignAlgorithm.SHA256withECDSA,
                Base64.decode(privateKeyBase64),
                null
        );

        return Base64.encode(sign.sign(data));
    }

    /**
     * 3. 服务端验证签名（核心）
     */
    public static boolean verify(
            String publicKeyBase64,
            String challenge,
            String signatureBase64
    ) {
        try {
            byte[] data = challenge.getBytes(StandardCharsets.UTF_8);
            byte[] signature = Base64.decode(signatureBase64);

            Sign sign = SecureUtil.sign(
                    SignAlgorithm.SHA256withECDSA,
                    null,
                    Base64.decode(publicKeyBase64)
            );

            return sign.verify(data, signature);
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        // ======================
        // 步骤1：生成 Passkey
        // ======================
        Map<String, String> passkey = PasskeyTest.generatePasskey();
        String credentialId = passkey.get("credentialId");
        String publicKey = passkey.get("publicKey");
        String privateKey = passkey.get("privateKey");

        System.out.println("credentialId: " + credentialId);
        System.out.println("公钥(存库): " + publicKey);


        // ======================
        // 步骤2：生成挑战值（后端）
        // ======================
        String challenge = "123456"; // 模拟随机挑战


        // ======================
        // 步骤3：模拟设备登录签名
        // ======================
        String signature = PasskeyTest.sign(privateKey, challenge);
        System.out.println("签名: " + signature);


        // ======================
        // 步骤4：后端验证签名 ✅
        // ======================
        boolean ok = PasskeyTest.verify(publicKey, challenge, signature);

        System.out.println("验证结果：" + ok);
    }
}
