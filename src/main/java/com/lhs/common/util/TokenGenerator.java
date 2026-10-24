package com.lhs.common.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 会话 token 生成器（不透明随机串，不依赖任何密钥）
 * <p>
 * token 本身不携带任何用户信息，服务端存储「token → uid」的映射作为唯一权威，
 * 因此无需对称加密或签名，密钥泄漏也无法伪造身份。校验路径只查存储，不做解析。
 * <p>
 * 格式：{@code ut_}/{@code at_} 前缀 + 43 字符 URL-safe Base64（256 bit 熵），
 * 前缀用于区分用户会话与管理会话，防止两端 token 互相越界使用。
 *
 * @author BackEndV3
 */
public final class TokenGenerator {

    /** 安全随机源：SecureRandom 线程安全，全局复用一个实例 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** token 随机部分字节数：32 字节 = 256 bit */
    private static final int TOKEN_BYTES = 32;

    /** URL-safe Base64 且去掉填充：不产生 + / = ，可直接放在 header 与 URL 中 */
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** 用户会话 token 前缀 */
    public static final String USER_PREFIX = "ut_";

    /** 管理端会话 token 前缀 */
    public static final String ADMIN_PREFIX = "at_";

    private TokenGenerator() {
    }

    /**
     * 生成用户会话 token
     *
     * @return 形如 ut_xxxxxxxx 的随机 token
     */
    public static String newUserToken() {
        return USER_PREFIX + random();
    }

    /**
     * 生成管理端会话 token
     *
     * @return 形如 at_xxxxxxxx 的随机 token
     */
    public static String newAdminToken() {
        return ADMIN_PREFIX + random();
    }

    /**
     * 判断是否为用户会话 token 格式
     *
     * @param token 待判断 token
     * @return 符合用户 token 格式返回 true
     */
    public static boolean isUserToken(String token) {
        return token != null && token.startsWith(USER_PREFIX);
    }

    /**
     * 判断是否为管理端会话 token 格式
     *
     * @param token 待判断 token
     * @return 符合管理端 token 格式返回 true
     */
    public static boolean isAdminToken(String token) {
        return token != null && token.startsWith(ADMIN_PREFIX);
    }

    /**
     * 生成 256 bit 随机串
     *
     * @return 43 字符 URL-safe Base64
     */
    private static String random() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
