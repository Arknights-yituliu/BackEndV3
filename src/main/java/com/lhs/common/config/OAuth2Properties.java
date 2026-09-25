package com.lhs.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统一用户中心接入配置
 * <p>
 * 对应 application-test.yml 中 user-center.oauth 配置项。
 * 授权码流程已下线，仅保留直连登录所需的 UC 地址与客户端凭证
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
}
