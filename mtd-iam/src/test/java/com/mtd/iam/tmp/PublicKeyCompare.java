package com.mtd.iam.tmp;

import cn.hutool.core.codec.Base64;

/**
 * 对比两种公钥编码格式
 */
public class PublicKeyCompare {
    public static void main(String[] args) {
        // 前端浏览器的公钥（Raw 格式）
        String frontendPubKey = "BPfcywZmjv159W/dMgeWzMRniL1ZqGYTJcSmtyuI602QchLZmyaQiGuZGHXY3nUIh7sxfN/h5pHohQ7pFhDtLRY=";
        
        // Java 输出的公钥（X.509 格式）
        String javaPubKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE99zLBmaO/Xn1b90yB5bMxGeIvVmoZhMlxKa3K4jrTZByEtmbJpCIa5kYddjedQiHuzF83+HmkeiFDukWEO0tFg==";
        
        byte[] frontendBytes = Base64.decode(frontendPubKey);
        byte[] javaBytes = Base64.decode(javaPubKey);
        
        System.out.println("=== 公钥格式对比 ===");
        System.out.println("前端公钥长度：" + frontendBytes.length + " bytes");
        System.out.println("Java 公钥长度：" + javaBytes.length + " bytes");
        
        System.out.println("\n=== 前端公钥结构（Raw 格式）===");
        System.out.println("第 1 字节 (未压缩标识): 0x" + String.format("%02X", frontendBytes[0]));
        System.out.println("X 坐标 (32 字节): " + bytesToHex(frontendBytes, 1, 32));
        System.out.println("Y 坐标 (32 字节): " + bytesToHex(frontendBytes, 33, 32));
        
        System.out.println("\n=== Java 公钥结构（X.509/SPKI 格式）===");
        System.out.println("ASN.1 SEQUENCE: 0x" + String.format("%02X", javaBytes[0]));
        System.out.println("总长度：" + javaBytes[1] + " bytes");
        System.out.println("包含：算法标识符 + BIT STRING（内含 Raw 公钥）");
        
        System.out.println("\n=== 提取 Java 公钥中的 X、Y 坐标 ===");
        // X.509 格式中，Raw 公钥在偏移位置
        // BIT STRING 从索引 27 开始，跳过 0x03 和长度字节，再跳过 0x00
        int rawStart = 27; // BIT STRING 内容起始位置
        if (javaBytes[rawStart] == 0x00) rawStart++; // 跳过填充字节
        
        System.out.println("从 Java 公钥提取的 Raw 部分: " + bytesToHex(javaBytes, rawStart, javaBytes.length - rawStart));
        System.out.println("前端 Raw 公钥：                    " + bytesToHex(frontendBytes, 0, frontendBytes.length));
        
        System.out.println("\n=== 验证是否一致 ===");
        boolean same = true;
        for (int i = 0; i < frontendBytes.length; i++) {
            if (frontendBytes[i] != javaBytes[rawStart + i]) {
                same = false;
                break;
            }
        }
        System.out.println("两个公钥的 Raw 部分是否相同？ " + (same ? "✅ 是" : "❌ 否"));
    }
    
    private static String bytesToHex(byte[] data, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length && i < data.length; i++) {
            sb.append(String.format("%02X", data[i]));
        }
        return sb.toString();
    }
}
