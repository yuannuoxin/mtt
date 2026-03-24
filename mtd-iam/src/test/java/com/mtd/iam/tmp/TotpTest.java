package com.mtd.iam.tmp;

import com.mtd.iam.util.TotpUtil;

public class TotpTest {
    public static void main(String[] args) throws InterruptedException {

        // 1. 生成密钥（存数据库）
        String secret = TotpUtil.generateSecret();
        System.out.println("TOTP 密钥(保存到数据库) = " + secret);

        // 2. 生成当前6位验证码
        String code = TotpUtil.generateCode(secret);
        System.out.println("当前6位验证码 = " + code);

        // 3. 验证
        boolean ok = TotpUtil.verifyCode(secret, code);
        System.out.println("验证结果 = " + ok);

        // 4. 30秒后会变化
        System.out.println("\n等待30秒后新验证码...");
        Thread.sleep(31000);

        String newCode = TotpUtil.generateCode(secret);
        System.out.println("新的6位验证码 = " + newCode);
    }
}
