package com.lhs.common.util;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类——bcrypt 单向哈希 + AES 旧密码兼容
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 对明文密码进行 bcrypt 哈希
     */
    public static String hash(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证明文密码是否与已存储的哈希匹配
     * 自动兼容旧的 AES 加密格式：如果已存储密码不以 $ 开头，则按 AES 方式比对
     *
     * @param rawPassword       用户输入的明文密码
     * @param storedPassword    数据库中存储的密码（可能是 bcrypt 哈希，也可能是旧 AES 密文）
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        // bcrypt 哈希始终以 $2 开头
        if (storedPassword.startsWith("$2")) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        // 旧 AES 格式：加密后比对
        try {
            String encrypted = AES.encrypt(rawPassword, ConfigUtil.Secret);
            return storedPassword.equals(encrypted);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断当前存储的密码是否为旧的 AES 格式（需要升级）
     */
    public static boolean isLegacyAES(String storedPassword) {
        return storedPassword != null && !storedPassword.isEmpty() && !storedPassword.startsWith("$2");
    }
}