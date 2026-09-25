package com.lhs.common.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 签名工具类：SHA-256 摘要与 Ed25519 签名
 *
 * <p>存量登录态迁移通过 Ed25519 非对称签名向 UC 认证（BackEndV3 持私钥签名、
 * UC 只存公钥验签），密钥无需在两台机器上各放一份。Ed25519 为 JDK 15+ 内置算法，
 * 无需引入第三方库。</p>
 *
 * @author BackEndV3
 */
public final class SignUtil {

    /** SHA-256 摘要算法名 */
    private static final String SHA_256 = "SHA-256";

    /** Ed25519 签名算法名（JDK 15+ 内置） */
    private static final String ED25519 = "Ed25519";

    private SignUtil() {
    }

    /**
     * 计算 SHA-256 摘要，输出小写十六进制字符串
     *
     * <p>用于生成「旧自签 token 摘要」，作为兑换缓存 key 与审计字段，
     * 原始 token 不落库、不入日志。</p>
     *
     * @param data 原文
     * @return 小写十六进制摘要；data 为 null 时返回 null
     */
    public static String sha256(String data) {
        if (data == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return bytesToHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 摘要计算失败", e);
        }
    }

    /**
     * 解析 Ed25519 私钥（Base64 编码的 PKCS#8 未加密私钥）
     *
     * @param privateKeyBase64 私钥（Base64 的 PKCS#8）
     * @return 私钥对象
     * @throws IllegalStateException 私钥为空或解析失败
     */
    public static PrivateKey parseEd25519PrivateKey(String privateKeyBase64) {
        if (privateKeyBase64 == null || privateKeyBase64.isBlank()) {
            throw new IllegalStateException("迁移签名私钥未配置");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64.trim());
            return KeyFactory.getInstance(ED25519).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new IllegalStateException("迁移签名私钥解析失败", e);
        }
    }

    /**
     * Ed25519 签名，输出 Base64 字符串
     *
     * @param privateKey Ed25519 私钥
     * @param data       待签名原文
     * @return Base64 签名
     * @throws IllegalStateException 签名失败
     */
    public static String ed25519Sign(PrivateKey privateKey, String data) {
        try {
            Signature signer = Signature.getInstance(ED25519);
            signer.initSign(privateKey);
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Ed25519 签名失败", e);
        }
    }

    /**
     * 字节数组转小写十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
