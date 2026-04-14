package com.mtd.common.web.aspect;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志请求响应配置
 *
 * @author woziji
 */
@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix = "request.log")
public class LogReqResConfig {

    private List<String> ignoreClasses = new ArrayList<>();

    private List<String> ignoreMethods = new ArrayList<>();
}
