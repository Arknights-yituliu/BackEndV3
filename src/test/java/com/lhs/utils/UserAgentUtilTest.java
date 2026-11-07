package com.lhs.utils;

import com.lhs.common.util.UserAgentUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UserAgentUtil 单元测试
 * <p>
 * 全部使用真实 UA 样本，重点锁住「标识重叠」导致的历史误判：
 * iOS 的 UA 含 Mac OS X、Android 含 Linux、微信 / Edge / Opera 含 Chrome。
 * 纯静态方法调用，不启动 Spring 上下文。
 */
@DisplayName("UA 解析")
class UserAgentUtilTest {

    private static final String UA_IPHONE_SAFARI = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    private static final String UA_IPAD = "Mozilla/5.0 (iPad; CPU OS 15_6 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.6 Mobile/15E148 Safari/604.1";

    private static final String UA_IPHONE_WECHAT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.42(0x18002a2b) "
            + "NetType/WIFI Language/zh_CN";

    private static final String UA_ANDROID_CHROME = "Mozilla/5.0 (Linux; Android 13; SM-S9080) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private static final String UA_ANDROID_TABLET = "Mozilla/5.0 (Linux; Android 13; SM-X700) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final String UA_ANDROID_ALIPAY = "Mozilla/5.0 (Linux; U; Android 12; zh-CN; "
            + "V2134A Build/SP1A.210812.003) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 "
            + "Chrome/69.0.3497.100 UWS/3.22.2.65 Mobile Safari/537.36 AlipayClient/10.3.0.6000";

    private static final String UA_ANDROID_QQ = "Mozilla/5.0 (Linux; Android 12; PDEM10) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 "
            + "MQQBrowser/6.2 TBS/045811 Mobile Safari/537.36 QQ/8.9.68.11255";

    private static final String UA_WIN_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final String UA_WIN_EDGE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.2210.91";

    private static final String UA_WIN_EDGE_LEGACY = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.19041";

    private static final String UA_WIN_OPERA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 OPR/105.0.0.0";

    private static final String UA_WIN7_IE11 = "Mozilla/5.0 (Windows NT 6.1; Trident/7.0; rv:11.0) like Gecko";

    private static final String UA_MAC_SAFARI = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";

    private static final String UA_MAC_CHROME = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final String UA_UBUNTU_FIREFOX = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:120.0) "
            + "Gecko/20100101 Firefox/120.0";

    private static final String UA_CHROME_OS = "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Test
    @DisplayName("iOS 设备不再被记成 Mac OS X")
    void iosShouldNotBeReportedAsMac() {
        assertEquals("iOS", UserAgentUtil.getOs(UA_IPHONE_SAFARI));
        assertEquals("iOS", UserAgentUtil.getOs(UA_IPAD));
        assertEquals("iOS", UserAgentUtil.getOs(UA_IPHONE_WECHAT));

        assertEquals("Safari", UserAgentUtil.getBrowser(UA_IPHONE_SAFARI));
        assertEquals("Mobile", UserAgentUtil.getDevice(UA_IPHONE_SAFARI));
        assertEquals("Tablet", UserAgentUtil.getDevice(UA_IPAD));
    }

    @Test
    @DisplayName("Android 设备不再被记成 Linux")
    void androidShouldNotBeReportedAsLinux() {
        assertEquals("Android", UserAgentUtil.getOs(UA_ANDROID_CHROME));
        assertEquals("Android", UserAgentUtil.getOs(UA_ANDROID_TABLET));

        assertEquals("Chrome", UserAgentUtil.getBrowser(UA_ANDROID_CHROME));
        assertEquals("Mobile", UserAgentUtil.getDevice(UA_ANDROID_CHROME));
        // Android 平板 UA 不含 Mobile，不能被算作手机
        assertEquals("Tablet", UserAgentUtil.getDevice(UA_ANDROID_TABLET));
    }

    @Test
    @DisplayName("Ubuntu 分支恢复可达")
    void ubuntuShouldBeReachable() {
        assertEquals("Ubuntu", UserAgentUtil.getOs(UA_UBUNTU_FIREFOX));
        assertEquals("Firefox", UserAgentUtil.getBrowser(UA_UBUNTU_FIREFOX));
        assertEquals("PC", UserAgentUtil.getDevice(UA_UBUNTU_FIREFOX));
    }

    @Test
    @DisplayName("应用内嵌浏览器优先于 Chrome / Safari")
    void embeddedBrowserShouldWinOverKernel() {
        assertEquals("WeChat", UserAgentUtil.getBrowser(UA_IPHONE_WECHAT));
        assertEquals("Alipay", UserAgentUtil.getBrowser(UA_ANDROID_ALIPAY));
        assertEquals("QQ", UserAgentUtil.getBrowser(UA_ANDROID_QQ));
    }

    @Test
    @DisplayName("Chromium 系桌面浏览器优先于 Chrome")
    void chromiumBasedBrowserShouldWinOverChrome() {
        assertEquals("Edge", UserAgentUtil.getBrowser(UA_WIN_EDGE));
        assertEquals("Edge", UserAgentUtil.getBrowser(UA_WIN_EDGE_LEGACY));
        assertEquals("Opera", UserAgentUtil.getBrowser(UA_WIN_OPERA));
        assertEquals("Chrome", UserAgentUtil.getBrowser(UA_WIN_CHROME));
    }

    @Test
    @DisplayName("macOS 上的 Chrome 不能被记成 Safari")
    void macChromeShouldNotBeReportedAsSafari() {
        assertEquals("Chrome", UserAgentUtil.getBrowser(UA_MAC_CHROME));
        assertEquals("Mac OS X", UserAgentUtil.getOs(UA_MAC_CHROME));
        assertEquals("PC", UserAgentUtil.getDevice(UA_MAC_CHROME));
    }

    @Test
    @DisplayName("Windows 版本与 IE 识别保持原口径")
    void windowsVersionShouldStayConsistent() {
        assertEquals("Windows 10", UserAgentUtil.getOs(UA_WIN_CHROME));
        assertEquals("Windows 8.1", UserAgentUtil.getOs(UA_WIN_EDGE_LEGACY));
        assertEquals("Windows 7", UserAgentUtil.getOs(UA_WIN7_IE11));

        assertEquals("IE", UserAgentUtil.getBrowser(UA_WIN7_IE11));
        assertEquals("PC", UserAgentUtil.getDevice(UA_WIN_CHROME));
    }

    @Test
    @DisplayName("Chrome OS 单独归类")
    void chromeOsShouldBeRecognized() {
        assertEquals("Chrome OS", UserAgentUtil.getOs(UA_CHROME_OS));
        assertEquals("Chrome", UserAgentUtil.getBrowser(UA_CHROME_OS));
        assertEquals("PC", UserAgentUtil.getDevice(UA_CHROME_OS));
    }

    @Test
    @DisplayName("空 UA 与无法识别的 UA 回落为 Unknown")
    void blankOrUnknownUaShouldFallBack() {
        assertEquals("Unknown", UserAgentUtil.getBrowser((String) null));
        assertEquals("Unknown", UserAgentUtil.getOs((String) null));
        assertEquals("Unknown", UserAgentUtil.getDevice((String) null));

        assertEquals("Unknown", UserAgentUtil.getBrowser(""));
        assertEquals("Unknown", UserAgentUtil.getOs("   "));
        assertEquals("Unknown", UserAgentUtil.getDevice(""));

        String curl = "curl/8.4.0";
        assertEquals("Unknown", UserAgentUtil.getBrowser(curl));
        assertEquals("Unknown", UserAgentUtil.getOs(curl));
        assertEquals("Unknown", UserAgentUtil.getDevice(curl));
    }
}
