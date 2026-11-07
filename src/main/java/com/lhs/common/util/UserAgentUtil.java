package com.lhs.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Locale;

/**
 * User-Agent解析工具类
 * 用于从请求中提取浏览器、操作系统、设备等信息
 * <p>
 * 每个维度都是一张<b>有序</b>规则表，自上而下第一个命中的 token 决定结果，
 * 因此「顺序即优先级」：越具体的标识必须排得越靠前。
 * <p>
 * UA 字符串里的标识大量重叠，按产品知名度排序会产生不可达分支，历史实现即栽在这里：
 * <ul>
 *   <li>iPhone 的 UA 是 {@code (iPhone; CPU iPhone OS 17_0 like Mac OS X)}，
 *       含 "Mac OS X"。macOS 规则先命中，所有 iOS 设备都被记成了 Mac OS X</li>
 *   <li>Android 的 UA 含 "Linux"，Ubuntu 的 UA 也含 "Linux"。
 *       "linux" 规则先命中，Android 与 Ubuntu 两个分支永远不可达</li>
 *   <li>微信 / QQ / 微博 / 支付宝 / Edge / Opera 都基于 Chromium 或 WebKit，
 *       UA 里同时带 "Chrome" 或 "Safari"。内核先命中，壳浏览器全部被记成 Chrome / Safari</li>
 * </ul>
 * 修改规则时只调整 {@link #BROWSER_RULES} / {@link #OS_RULES} 的顺序，
 * 不要再退回成散落的 if-else，否则顺序会被下一次改动悄悄打乱。
 */
public class UserAgentUtil {

    /** 解析失败时的统一返回值 */
    private static final String UNKNOWN = "Unknown";

    /**
     * 浏览器识别规则（顺序即优先级）
     * <p>
     * 带版本号的 token 一律保留分隔符（{@code edg/}、{@code opr/}、{@code qq/}），
     * 避免裸词误伤其他 UA 中的子串；老版本没有斜杠的写法额外列一条。
     * <p>
     * 返回值与原实现保持一致，避免前端已有的图标 / 颜色映射失效。
     */
    private static final List<Rule> BROWSER_RULES = List.of(
            // 应用内嵌浏览器优先：UA 里一定带宿主内核（Chrome / Safari）的标识
            new Rule("WeChat", "micromessenger"),
            new Rule("QQ", "qq/", "qqbrowser"),
            new Rule("Weibo", "weibo"),
            new Rule("Alipay", "alipay", "aliapp"),
            // 基于 Chromium 的桌面浏览器：UA 同时含 "Chrome"
            new Rule("Edge", "edg/", "edge/"),
            new Rule("Opera", "opr/", "opera"),
            // 内核浏览器
            new Rule("IE", "msie", "trident"),
            new Rule("Firefox", "firefox", "fxios"),
            new Rule("Chrome", "chrome", "crios", "crmo"),
            new Rule("Safari", "safari")
    );

    /**
     * 操作系统识别规则（顺序即优先级）
     * <p>
     * 移动端整体排在桌面端之前，Ubuntu 排在通用 Linux 之前，理由见类注释。
     */
    private static final List<Rule> OS_RULES = List.of(
            new Rule("iOS", "iphone", "ipad", "ipod"),
            new Rule("Android", "android"),
            // Windows 版本号从新到旧；NT 10.0 同时覆盖 Windows 11，UA 层面无法区分
            new Rule("Windows 10", "windows nt 10"),
            new Rule("Windows 8.1", "windows nt 6.3"),
            new Rule("Windows 8", "windows nt 6.2"),
            new Rule("Windows 7", "windows nt 6.1"),
            new Rule("Windows Vista", "windows nt 6.0"),
            new Rule("Windows XP", "windows nt 5.1", "windows xp"),
            // Ubuntu 的 UA 含 "Linux"，必须早于通用 linux
            new Rule("Ubuntu", "ubuntu"),
            new Rule("Chrome OS", "cros"),
            new Rule("Mac OS X", "mac os", "macintosh"),
            new Rule("Linux", "linux"),
            // 兜底：Windows Phone 等未单独列出的 Windows 版本
            new Rule("Windows", "windows")
    );

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
        return getBrowser(getUserAgent(request));
    }

    /**
     * 获取浏览器信息
     */
    public static String getBrowser(String userAgent) {
        return match(BROWSER_RULES, userAgent);
    }

    /**
     * 获取操作系统信息
     */
    public static String getOs(HttpServletRequest request) {
        return getOs(getUserAgent(request));
    }

    /**
     * 获取操作系统信息
     */
    public static String getOs(String userAgent) {
        return match(OS_RULES, userAgent);
    }

    /**
     * 获取设备类型
     */
    public static String getDevice(HttpServletRequest request) {
        return getDevice(getUserAgent(request));
    }

    /**
     * 获取设备类型
     * <p>
     * 先判平板再判手机：Android 平板 UA 只含 "Android" 不含 "Mobile"，
     * 若把 "android" 和 "mobile" 放在同一档，平板会被算成手机。
     * <p>
     * iOS 13 起 iPad 默认发送桌面版 UA（{@code Macintosh}），这里只能归为 PC；
     * 要精确区分需要前端配合上报 {@code Sec-CH-UA-Mobile} 客户端提示。
     */
    public static String getDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);

        if (ua.contains("ipad") || ua.contains("tablet")
                || (ua.contains("android") && !ua.contains("mobile"))) {
            return "Tablet";
        }
        if (ua.contains("mobile") || ua.contains("android")
                || ua.contains("iphone") || ua.contains("ipod")) {
            return "Mobile";
        }
        if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("mac os")
                || ua.contains("linux") || ua.contains("cros")) {
            return "PC";
        }
        return UNKNOWN;
    }

    /**
     * 按规则表顺序匹配，返回第一个命中的结果
     *
     * @param rules     有序规则表
     * @param userAgent 原始 UA
     * @return 命中的结果，全部未命中返回 "Unknown"
     */
    private static String match(List<Rule> rules, String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN;
        }
        // 固定用 Locale.ROOT：默认 locale 在土耳其语环境下会把 "I" 转成 "ı"，
        // "iPhone" 变成 "ıphone" 后 contains("iphone") 直接失配
        String ua = userAgent.toLowerCase(Locale.ROOT);
        for (Rule rule : rules) {
            if (rule.matches(ua)) {
                return rule.result();
            }
        }
        return UNKNOWN;
    }

    /**
     * 一条 UA 匹配规则
     *
     * @param result 命中后返回的结果
     * @param tokens 任一 token 出现在 UA 中即视为命中（调用方保证 UA 已转小写）
     */
    private record Rule(String result, String... tokens) {

        boolean matches(String lowerCaseUa) {
            for (String token : tokens) {
                if (lowerCaseUa.contains(token)) {
                    return true;
                }
            }
            return false;
        }
    }
}
