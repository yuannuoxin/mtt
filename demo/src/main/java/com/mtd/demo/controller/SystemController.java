package com.mtd.demo.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.mtd.common.core.result.Result;
import com.mtd.demo.response.base.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HttpServletRequest request;

    // 版本信息（从 version.properties 读取）
    private String projectVersion = "unknown";
    private String springBootVersion = "unknown";
    private String springCloudVersion = "unknown";
    private String buildTime = "unknown";
    private String gitBranch = "unknown";
    private String gitCommitId = "unknown";
    private String gitCommitTime = "unknown";

    public SystemController() {
        try {
            var resource = new ClassPathResource("version.properties");
            if (resource.exists()) {
                var props = new java.util.Properties();
                props.load(resource.getInputStream());
                this.projectVersion = props.getProperty("project.version", "unknown");
                this.springBootVersion = props.getProperty("spring-boot.version", "unknown");
                this.springCloudVersion = props.getProperty("spring-cloud.version", "unknown");
                this.buildTime = props.getProperty("build.time", "unknown");
                this.gitBranch = props.getProperty("git.branch", "unknown");
                this.gitCommitId = props.getProperty("git.commit.id", "unknown");
                this.gitCommitTime = props.getProperty("git.commit.time", "unknown");
            }
        } catch (Exception e) {
            log.warn("Failed to load version.properties", e);
        }
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
     * 获取 JAR 包名称
     *
     * @return JAR 包信息
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

    @Operation(summary = "获取所有常见日期类型", description = "返回各种常用的日期时间类型示例")
    @PostMapping("/date-types")
    public Result<DateTypesResponse> getAllDateTypes() {
        DateTypesResponse response = new DateTypesResponse();

        // 当前时刻作为基准时间
        long nowMillis = System.currentTimeMillis();

        // java.util.Date
        response.setUtilDate(new Date(nowMillis));

        // java.sql.Date (只包含日期部分)
        response.setSqlDate(new Date(nowMillis));

        // java.sql.Timestamp (精确到纳秒)
        response.setTimestamp(new Timestamp(nowMillis));

        // java.sql.Time (只包含时间部分)
        response.setTime(new Time(nowMillis));

        // Java 8+ LocalDate
        response.setLocalDate(LocalDate.now());

        // Java 8+ LocalTime
        response.setLocalTime(LocalTime.now());

        // Java 8+ LocalDateTime
        response.setLocalDateTime(LocalDateTime.now());

        // Java 8+ Instant (时间戳)
        response.setInstant(Instant.now());

        // Java 8+ ZonedDateTime (系统默认时区)
        response.setZonedDateTime(ZonedDateTime.now());

        // Java 8+ OffsetDateTime
        response.setOffsetDateTime(OffsetDateTime.now());

        // Java 8+ YearMonth
        response.setYearMonth(YearMonth.now());

        // Java 8+ MonthDay
        response.setMonthDay(MonthDay.now());

        // Java 8+ Year
        response.setYear(Year.now());

        // Hutool DateTime
        response.setHutoolDateTime(DateUtil.date());

        // 毫秒时间戳
        response.setCurrentTimeMillis(System.currentTimeMillis());

        // 纳秒时间戳
        response.setNanoTime(System.nanoTime());

        return Result.success(response);
    }

    @Operation(summary = "Jackson 序列化测试", description = "验证 Long 转 String 和日期时间格式化是否正确")
    @PostMapping("/test-jackson-serialization")
    public Result<JacksonSerializationTestResponse> testJacksonSerialization() {
        JacksonSerializationTestResponse response = new JacksonSerializationTestResponse();

        // 设置一个较大的 Long 值（测试是否转为 String）
        Long testLong = 1234567890123456789L;
        response.setLongValue(testLong);
        response.setLongPrimitive(testLong);

        // 设置 Integer 值（应保持为数字）
        response.setIntegerValue(12345);

        // 设置日期时间值
        LocalDateTime now = LocalDateTime.now();
        response.setLocalDateTime(now);
        response.setLocalDate(now.toLocalDate());
        response.setLocalTime(now.toLocalTime());

        // 设置传统 Date 类型
        Date date = new Date();
        response.setUtilDate(date);
        response.setTimestamp(new Timestamp(date.getTime()));

        // 设置 BigInteger 和 BigDecimal
        response.setBigInteger(new BigInteger("123456789012345678901234567890"));
        response.setBigDecimal(new BigDecimal("123456789.12345678901234567890"));

        // 添加说明
        response.setNote("如果配置正确：Long/BigInteger/BigDecimal 应为字符串格式，日期时间应为 'yyyy-MM-dd HH:mm:ss' 格式");

        return Result.success(response);
    }

    @Operation(summary = "获取 Git 和构建信息", description = "获取当前版本的 Git 分支、提交时间、提交 ID 以及打包时间等信息")
    @PostMapping("/git-build-info")
    public Result<GitBuildInfoResponse> getGitBuildInfo() {
        GitBuildInfoResponse response = new GitBuildInfoResponse();
        response.setGitBranch(gitBranch);
        response.setGitCommitId(gitCommitId);
        response.setGitCommitTime(gitCommitTime);
        response.setBuildTime(buildTime);
        response.setProjectVersion(projectVersion);

        return Result.success(response);
    }

    @Operation(summary = "获取时区与语言信息", description = "收集客户端、服务器和数据库的时区、语言等配置信息")
    @PostMapping("/timezone-language-info")
    public Result<TimeZoneLanguageResponse> getTimeZoneLanguageInfo() {

        TimeZoneLanguageResponse response = new TimeZoneLanguageResponse();

        // 1. 客户端信息 - 从 HTTP 请求中获取
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setClientIp(JakartaServletUtil.getClientIP(request));
        clientInfo.setClientLanguage(request.getHeader("Accept-Language"));
        clientInfo.setClientUserAgent(request.getHeader("User-Agent"));
        clientInfo.setTimezone(null); // 前端可通过请求体传递
        clientInfo.setNote("客户端信息从 HTTP 请求头自动获取");
        response.setClient(clientInfo);

        // 2. 服务器信息
        ServerInfo serverInfo = new ServerInfo();
        TimeZone defaultTz = TimeZone.getDefault();
        Locale defaultLocale = Locale.getDefault();

        serverInfo.setTimezone(defaultTz.getID());
        serverInfo.setTimezoneOffset(getTimezoneOffset(defaultTz));
        serverInfo.setJvmTimezone(System.getProperty("user.timezone", "未知"));
        serverInfo.setJvmLocale(defaultLocale.toString() + " (" + defaultLocale.getDisplayName() + ")");
        serverInfo.setJvmOsName(System.getProperty("os.name", "未知"));
        serverInfo.setJvmOsArch(System.getProperty("os.arch", "未知"));
        serverInfo.setNote("服务器 JVM 信息，启动时可配置 -Duser.timezone 和 -Duser.language");
        response.setServer(serverInfo);

        // 3. MySQL 数据库信息
        DatabaseInfo dbInfo = new DatabaseInfo();
        try {
            // 使用 JdbcTemplate 查询 MySQL 时区和字符集
            org.springframework.jdbc.core.RowMapper<Map<String, String>> rowMapper = (rs, rowNum) -> Map.of(
                    "tz", rs.getString("tz"),
                    "sys_tz", rs.getString("sys_tz"),
                    "charset", rs.getString("charset")
            );

            var result = jdbcTemplate.queryForObject(
                    "SELECT @@time_zone AS tz, @@system_time_zone AS sys_tz, @@character_set_database AS charset",
                    rowMapper
            );

            if (result != null) {
                String tz = result.get("tz");
                String sysTz = result.get("sys_tz");
                String charset = result.get("charset");

                // 调试：打印原始值
                log.info("MySQL 时区原始值 - tz: {}, sys_tz: {}, charset: {}", tz, sysTz, charset);

                dbInfo.setDbTimezone(tz);
                dbInfo.setDbSystemTimezone(sysTz);
                dbInfo.setDbCharset(charset);
            } else {
                dbInfo.setDbTimezone("查询失败");
                dbInfo.setDbSystemTimezone("查询失败");
                dbInfo.setDbCharset("查询失败");
            }
        } catch (Exception e) {
            log.warn("获取数据库时区信息失败：{}", e.getMessage());
            dbInfo.setDbTimezone("Asia/Shanghai (从 JDBC 配置推断)");
            dbInfo.setDbSystemTimezone("未知");
            dbInfo.setDbCharset("utf8mb4 (默认)");
        }
        dbInfo.setNote("MySQL 时区可通过 JDBC URL 的 serverTimezone 参数指定，或在 my.cnf 中配置 default-time-zone");
        response.setDatabase(dbInfo);

        return Result.success(response);
    }

    /**
     * 格式化时区偏移量
     */
    private String getTimezoneOffset(TimeZone tz) {
        int offset = tz.getRawOffset() / 1000 / 60; // 转换为分钟
        int hours = Math.abs(offset) / 60;
        int minutes = Math.abs(offset) % 60;
        String sign = offset >= 0 ? "+" : "-";
        return String.format("GMT%s%02d:%02d", sign, hours, minutes);
    }
}
