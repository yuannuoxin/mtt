package com.mtd.tt.util;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 登录设备信息工具：取请求 IP / UA，解析设备名称，生成对外设备 ID。
 */
public final class DeviceUtil {

    private DeviceUtil() {
    }

    /** 客户端 IP：优先 X-Forwarded-For 首段，其次 remoteAddr，最长 64 */
    public static String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            return truncate(first, 64);
        }
        return truncate(req.getRemoteAddr(), 64);
    }

    /** 当前请求 User-Agent，最长 255 */
    public static String userAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return truncate(attrs.getRequest().getHeader("User-Agent"), 255);
    }

    /** 由 UA 解析出可读设备名，如「Chrome · Windows」「手机 · Safari · iOS」 */
    public static String deviceName(String uaStr) {
        if (uaStr == null || uaStr.isBlank()) {
            return "未知设备";
        }
        try {
            UserAgent ua = UserAgentUtil.parse(uaStr);
            if (ua == null) {
                return "未知设备";
            }
            String browser = ua.getBrowser() == null ? "" : ua.getBrowser().getName();
            String os = ua.getOs() == null ? "" : ua.getOs().getName();
            if (browser.isBlank() && os.isBlank()) {
                return "未知设备";
            }
            StringBuilder sb = new StringBuilder();
            if (ua.isMobile()) {
                sb.append("手机 · ");
            }
            sb.append(browser.isBlank() ? "浏览器" : browser);
            if (!os.isBlank()) {
                sb.append(" · ").append(os);
            }
            return truncate(sb.toString(), 100);
        } catch (Exception e) {
            return "未知设备";
        }
    }

    /**
     * 对外的设备 ID：token 的 SHA-256 前 12 位。
     * 不直接暴露 token，前端凭此 ID 才能下线对应设备。
     */
    public static String deviceId(String token) {
        if (token == null) {
            return null;
        }
        return DigestUtil.sha256Hex(token).substring(0, 12);
    }

    public static String browserName(String uaStr) {
        try {
            UserAgent ua = uaStr == null ? null : UserAgentUtil.parse(uaStr);
            return ua == null || ua.getBrowser() == null ? null : truncate(ua.getBrowser().getName(), 40);
        } catch (Exception e) {
            return null;
        }
    }

    public static String osName(String uaStr) {
        try {
            UserAgent ua = uaStr == null ? null : UserAgentUtil.parse(uaStr);
            return ua == null || ua.getOs() == null ? null : truncate(ua.getOs().getName(), 40);
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String v, int max) {
        if (v == null) {
            return null;
        }
        return v.length() > max ? v.substring(0, max) : v;
    }
}
