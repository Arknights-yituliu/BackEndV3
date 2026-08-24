package com.lhs.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE（RFC 7636）S256 参数生成工具
 */
public class PkceUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成随机 code_verifier（43 位）
     *
     * @return code_verifier 字符串
     */
    public static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 由 code_verifier 计算 code_challenge（S256，RFC 7636）
     *
     * @param codeVerifier 随机串
     * @return code_challenge 字符串
     */
    public static String s256(String codeVerifier) throws Exception {
        // 1. SHA-256 摘要
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        // 2. BASE64URL 编码（无 padding）
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
