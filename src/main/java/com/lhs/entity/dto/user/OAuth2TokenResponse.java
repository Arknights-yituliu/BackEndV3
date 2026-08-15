package com.lhs.entity.dto.user;

/**
 * OAuth2 令牌响应（RFC 6749 标准字段）
 * <p>
 * 对应 UC /oauth2/token 接口 data 部分
 */
public class OAuth2TokenResponse {

    /** 访问令牌 */
    private String accessToken;

    /** 令牌类型，固定 Bearer */
    private String tokenType;

    /** 访问令牌有效期（秒） */
    private Long expiresIn;

    /** 刷新令牌 */
    private String refreshToken;

    /** 授权范围 */
    private String scope;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
