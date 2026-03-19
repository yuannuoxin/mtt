package com.mtd.demo.response.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务器信息
 */
@Data
public class ServerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 服务器时区
     */
    private String timezone;

    /**
     * 服务器时区偏移量
     */
    private String timezoneOffset;

    /**
     * JVM 时区
     */
    private String jvmTimezone;

    /**
     * JVM 语言环境
     */
    private String jvmLocale;

    /**
     * 操作系统名称
     */
    private String jvmOsName;

    /**
     * 操作系统架构
     */
    private String jvmOsArch;

    /**
     * 备注
     */
    private String note;
}
