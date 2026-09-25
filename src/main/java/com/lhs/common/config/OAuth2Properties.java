package com.lhs.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统一用户中心接入配置
 * <p>
 * 对应 application-test.yml 中 user-center.oauth 配置项。
 * 授权码流程已下线，仅保留直连登录所需的 UC 地址与客户端凭证，
 * 以及存量登录态迁移（自签 token → UC 令牌按需兑换）所需的签名配置
 */
@Component
@ConfigurationProperties(prefix = "user-center.oauth")
public class OAuth2Properties {

    /** UC 服务根地址，如 http://localhost:8080（/oauth2/** 的根） */
    private String baseUrl;

    /** 客户端 ID（对应 UC oauth_client 表 id） */
    private String clientId;

    /** 客户端密钥（明文，仅服务端持有，入库为 BCrypt） */
    private String clientSecret;

    /** 存量登录态迁移配置 */
    private Migrate migrate = new Migrate();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Migrate getMigrate() {
        return migrate;
    }

    public void setMigrate(Migrate migrate) {
        this.migrate = migrate;
    }

    /**
     * 存量登录态迁移配置（对应 user-center.oauth.migrate）
     */
    public static class Migrate {

        /** 总开关：默认关闭，上线时显式打开，存量迁移完成后立即关闭 */
        private boolean enabled = false;

        /** 当前生效的签名私钥标识（对应 UC 侧 kid → 公钥映射表） */
        private String kid = "k1";

        /** Ed25519 签名私钥（PKCS#8 未加密，Base64）；仅本侧持有，严禁进入日志 */
        private String privateKey;

        /** 允许的请求时间偏移（秒），须与 UC 侧 clock-skew-seconds 一致 */
        private long clockSkewSeconds = 120L;

        /** 来源标识，仅用于 UC 侧审计 */
        private String origin = "backendv3-legacy";

        /** 单次调用 UC 的超时（秒） */
        private long timeoutSeconds = 3L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public long getClockSkewSeconds() {
            return clockSkewSeconds;
        }

        public void setClockSkewSeconds(long clockSkewSeconds) {
            this.clockSkewSeconds = clockSkewSeconds;
        }

        public String getOrigin() {
            return origin;
        }

        public void setOrigin(String origin) {
            this.origin = origin;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
