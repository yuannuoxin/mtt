package com.mtd.demo.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.mtd.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

/**
 * 系统信息控制器
 */
@Slf4j
@Tag(name = "系统信息", description = "系统运行状态相关接口")
@RestController
@RequestMapping("/system")
public class SystemController {

    @Value("${spring.application.name:application}")
    private String applicationName;

    // 版本信息（从 version.properties 读取）
    private String projectVersion = "unknown";
    private String springBootVersion = "unknown";
    private String springCloudVersion = "unknown";

    public SystemController() {
        try {
            var resource = new ClassPathResource("version.properties");
            if (resource.exists()) {
                var props = new java.util.Properties();
                props.load(resource.getInputStream());
                this.projectVersion = props.getProperty("project.version", "unknown");
                this.springBootVersion = props.getProperty("spring-boot.version", "unknown");
                this.springCloudVersion = props.getProperty("spring-cloud.version", "unknown");
            }
        } catch (Exception e) {
            log.warn("Failed to load version.properties", e);
        }
    }

    /**
     * 系统信息响应对象
     */
    @Data
    public static class SystemInfoResponse implements Serializable {
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

    @Operation(summary = "获取系统信息", description = "获取应用启动时间、当前时间和 JAR 包名称等系统信息")
    @PostMapping("/info")
    public Result<SystemInfoResponse> getSystemInfo() {
        // 获取 JVM 启动时间
        long startupTimeMillis = java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
        DateTime startTime = new DateTime(startupTimeMillis);

        // 获取当前时间
        DateTime currentTime = DateUtil.date();

        // 计算运行时长（秒）
        long uptimeSeconds = (System.currentTimeMillis() - startupTimeMillis) / 1000;

        // 获取 JAR 包名称和路径信息
        JarInfo jarInfo = getJarInfo();

        // 构建响应对象
        SystemInfoResponse response = new SystemInfoResponse();
        response.setStartTime(DateUtil.formatDateTime(startTime));
        response.setCurrentTime(DateUtil.formatDateTime(currentTime));
        response.setJarName(jarInfo.getJarName());
        response.setApplicationName(applicationName);
        response.setProjectVersion(projectVersion);
        response.setSpringBootVersion(springBootVersion);
        response.setSpringCloudVersion(springCloudVersion);
        response.setUptimeSeconds(uptimeSeconds);
        response.setJarPathRaw(jarInfo.getJarPathRaw());
        response.setJarPathDecoded(jarInfo.getJarPathDecoded());

        return Result.success(response);
    }

    /**
     * JAR 信息封装类
     */
    @Data
    private static class JarInfo implements Serializable {
        private String jarName;
        private String jarPathRaw;
        private String jarPathDecoded;
    }

    /**
     * 获取 JAR 包名称
     *
     * @return JAR 包名称
     */
    private JarInfo getJarInfo() {
        JarInfo info = new JarInfo();
        
        try {
            // 获取项目路径
            java.net.URL location = SystemController.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            
            if (location == null) {
                log.warn("CodeSource location is null");
                info.setJarName("Running in IDE mode (not packaged as JAR)");
                info.setJarPathRaw("null");
                info.setJarPathDecoded("null");
                return info;
            }
            
            String path = location.getPath();
            String decodedPath = java.net.URLDecoder.decode(path, "UTF-8");
            
            info.setJarPathRaw(path);
            info.setJarPathDecoded(decodedPath);
            
            log.info("JAR path raw: {}", path);
            log.info("JAR path decoded: {}", decodedPath);
            
            // 处理 Spring Boot 嵌套 JAR 的情况
            if (decodedPath.startsWith("nested:")) {
                // 提取主 JAR 路径 nested:/C:/xxx.jar/!BOOT-INF/classes/!/ -> C:/xxx.jar
                int jarStart = decodedPath.indexOf(":/");
                int jarEnd = decodedPath.indexOf(".jar!");
                log.info("Jar start index: {}, end index: {}", jarStart, jarEnd);
                if (jarStart >= 0 && jarEnd > jarStart) {
                    String jarPath = decodedPath.substring(jarStart + 2, jarEnd + 4);
                    log.info("Extracted jar path: {}", jarPath);
                    jarPath = jarPath.replace("\\", "/");
                    int lastSlash = jarPath.lastIndexOf("/");
                    if (lastSlash >= 0 && lastSlash < jarPath.length() - 1) {
                        String fileName = jarPath.substring(lastSlash + 1);
                        log.info("Extracted nested jar name: {}", fileName);
                        info.setJarName(fileName);
                        return info;
                    }
                }
                
                // 备用方案：直接查找 .jar
                int jarIndex = decodedPath.indexOf(".jar");
                if (jarIndex > 0) {
                    String tempPath = decodedPath.substring(0, jarIndex + 4);
                    int slashIndex = tempPath.lastIndexOf("/");
                    if (slashIndex >= 0 && slashIndex < tempPath.length() - 1) {
                        String fileName = tempPath.substring(slashIndex + 1);
                        log.info("Extracted jar name (backup): {}", fileName);
                        info.setJarName(fileName);
                        return info;
                    }
                }
            }
            
            // 处理 Windows 路径（将反斜杠转换为正斜杠）
            decodedPath = decodedPath.replace("\\", "/");
            
            // 提取文件名
            int lastSlash = decodedPath.lastIndexOf("/");
            if (lastSlash >= 0 && lastSlash < decodedPath.length() - 1) {
                String fileName = decodedPath.substring(lastSlash + 1);
                log.info("Extracted file name: {}", fileName);
                // 如果是 JAR 文件，返回 JAR 名称
                if (fileName.endsWith(".jar")) {
                    info.setJarName(fileName);
                    return info;
                }
            }
            
            // 尝试从 URI 获取
            java.net.URI uri = location.toURI();
            String uriPath = uri.getPath();
            if (uriPath != null) {
                log.info("URI path: {}", uriPath);
                
                uriPath = java.net.URLDecoder.decode(uriPath, "UTF-8");
                uriPath = uriPath.replace("\\", "/");
                
                int lastUriSlash = uriPath.lastIndexOf("/");
                if (lastUriSlash >= 0 && lastUriSlash < uriPath.length() - 1) {
                    String fileName = uriPath.substring(lastUriSlash + 1);
                    if (fileName.endsWith(".jar")) {
                        info.setJarName(fileName);
                        return info;
                    }
                }
            }
            
            // 如果不是 JAR 模式，返回类路径提示
            info.setJarName("Running in IDE mode (not packaged as JAR)");
            return info;
        } catch (Exception e) {
            log.error("Failed to get jar name", e);
            info.setJarName("Unknown");
            return info;
        }
    }
}
