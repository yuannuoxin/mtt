package com.mtd.demo.response.base;

import lombok.Data;

import java.io.Serializable;

/**
 * JAR 信息封装类
 */
@Data
public class JarInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JAR 包名称
     */
    private String jarName;

    /**
     * JAR 路径原始值
     */
    private String jarPathRaw;

    /**
     * JAR 路径解码后
     */
    private String jarPathDecoded;
}
