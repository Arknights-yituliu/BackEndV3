package com.lhs.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统一用户中心 OAuth2 接入配置
 * <p>
 * 对应 application-test.yml 中 user-center.oauth 配置项
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

    /** 授权回调地址，必须与 UC 登记白名单精确匹配 */
    private String redirectUri;

    /** 授权范围，如 user.read */
    private String scope;

    /** UC 登录页地址，未登录时兜底跳转 */
    private String loginPageUrl;

    /** 授权成功后的前端回跳地址（可选）；配置后回调接口将 302 跳转并携带 token，未配置则返回 JSON */
    private String frontendRedirectUrl;

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

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getLoginPageUrl() {
        return loginPageUrl;
    }

    public void setLoginPageUrl(String loginPageUrl) {
        this.loginPageUrl = loginPageUrl;
    }

    public String getFrontendRedirectUrl() {
        return frontendRedirectUrl;
    }

    public void setFrontendRedirectUrl(String frontendRedirectUrl) {
        this.frontendRedirectUrl = frontendRedirectUrl;
    }
}
