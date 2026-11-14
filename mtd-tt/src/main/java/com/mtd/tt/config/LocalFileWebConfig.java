package com.mtd.tt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地存储模式的静态资源映射：GET /files/** 直接读取 {file.local.root} 下的文件。
 * 仅在 file.storage.type=local 时实际有内容可读，minio 模式下该路径返回 404。
 */
@Configuration
public class LocalFileWebConfig implements WebMvcConfigurer {

    @Value("${file.local.root:./data/files}")
    private String localRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(localRoot).toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
