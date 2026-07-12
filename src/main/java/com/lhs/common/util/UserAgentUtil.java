package com.lhs.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * User-Agent解析工具类
 * 用于从请求中提取浏览器、操作系统、设备等信息
 */
public class UserAgentUtil {

    /**
     * 获取User-Agent字符串
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * 获取浏览器信息
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return "Unknown";
        }
        return getBrowser(userAgent);
    }

    /**
     * 获取浏览器信息
     */
    public static String getBrowser(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("edge")) {
            return "Edge";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari")) {
            return "Safari";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        } else if (userAgent.contains("msie") || userAgent.contains("trident")) {
            return "IE";
        } else if (userAgent.contains("micromessenger")) {
            return "WeChat";
        } else if (userAgent.contains("qq/")) {
            return "QQ";
        } else if (userAgent.contains("weibo")) {
            return "Weibo";
        } else if (userAgent.contains("alipay")) {
            return "Alipay";
        }

        return "Unknown";
    }

    /**
     * 获取操作系统信息
     */
    public static String getOs(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return "Unknown";
        }
        return getOs(userAgent);
    }

    /**
     * 获取操作系统信息
     */
    public static String getOs(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("windows nt 10")) {
            return "Windows 10";
        } else if (userAgent.contains("windows nt 6.3")) {
            return "Windows 8.1";
        } else if (userAgent.contains("windows nt 6.2")) {
            return "Windows 8";
        } else if (userAgent.contains("windows nt 6.1")) {
            return "Windows 7";
        } else if (userAgent.contains("windows nt 6.0")) {
            return "Windows Vista";
        } else if (userAgent.contains("windows nt 5.1") || userAgent.contains("windows xp")) {
            return "Windows XP";
        } else if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac os x")) {
            return "Mac OS X";
        } else if (userAgent.contains("macintosh") || userAgent.contains("mac os")) {
            return "Mac OS";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        } else if (userAgent.contains("ubuntu")) {
            return "Ubuntu";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone")) {
            return "iOS";
        } else if (userAgent.contains("ipad")) {
            return "iOS";
        } else if (userAgent.contains("ios")) {
            return "iOS";
        }

        return "Unknown";
    }

    /**
     * 获取设备类型
     */
    public static String getDevice(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return "Unknown";
        }
        return getDevice(userAgent);
    }

    /**
     * 获取设备类型
     */
    public static String getDevice(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("mobile") || userAgent.contains("android") ||
            userAgent.contains("iphone") || userAgent.contains("ipod")) {
            return "Mobile";
        } else if (userAgent.contains("ipad") || userAgent.contains("tablet")) {
            return "Tablet";
        } else if (userAgent.contains("windows") || userAgent.contains("macintosh") ||
                   userAgent.contains("linux")) {
            return "PC";
        }

        return "Unknown";
    }
}
