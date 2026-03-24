package com.mtd.iam.util;

import cn.hutool.core.codec.Base32;
import cn.hutool.crypto.SecureUtil;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * TOTP 工具类（谷歌验证器标准）
 */
public class TotpUtil {

    private static final String ALGORITHM = "HmacSHA1";
    private static final int DIGITS = 6;        // 6位数字
    private static final int PERIOD = 30;      // 30秒过期
    private static final int SECRET_BYTES = 10;// 10字节 = 80位（标准）

    /**
     * 生成 TOTP 密钥（存数据库 user_mfa.secret）
     */
    public static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base32.encode(bytes);
    }

    /**
     * 根据密钥生成当前 6 位验证码
     */
    public static String generateCode(String secret) {
        byte[] keyBytes = Base32.decode(secret);
        long time = System.currentTimeMillis() / 1000 / PERIOD;
        byte[] data = longToBytes(time);
        byte[] hash = hmacSha1(keyBytes, data);
        int offset = hash[hash.length - 1] & 0xF;
        int code = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        code %= (int) Math.pow(10, DIGITS);
        return String.format("%06d", code);
    }

    /**
     * 验证用户输入的验证码是否正确
     */
    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != DIGITS) {
            return false;
        }
        String current = generateCode(secret);
        return current.equals(code);
    }

    // ==================== 内部工具 ====================
    private static byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }
}