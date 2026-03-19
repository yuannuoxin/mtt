package com.mtd.demo.response.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统信息响应对象
 */
@Data
public class SystemInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用启动时间
     */
    private String startTime;

    /**
     * 当前时间
     */
    private String currentTime;

    /**
     * JAR 包名称
     */
    private String jarName;

    /**
     * 应用名称
     */
    private String applicationName;

    /**
     * 项目版本
     */
    private String projectVersion;

    /**
     * Spring Boot 版本
     */
    private String springBootVersion;

    /**
     * Spring Cloud 版本
     */
    private String springCloudVersion;

    /**
     * 运行时长（秒）
     */
    private Long uptimeSeconds;

    /**
     * JAR 路径原始值
     */
    private String jarPathRaw;

    /**
     * JAR 路径解码后
     */
    private String jarPathDecoded;
}
