package com.lhs.entity.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UC 令牌对（服务端间传输与缓存载体）
 *
 * <p>同时用于三处：① 解析 UC /oauth2/internal/migrate-token 与 /oauth2/token 的响应；
 * ② 作为 Redis 兑换缓存的 JSON 值；③ 刷新后回带前端。refresh_token 只在本类与
 * Redis 缓存中存在，绝不下发浏览器。</p>
 *
 * <p>字段命名与 UC 响应保持一致（snake_case），避免多一层映射。</p>
 *
 * @author BackEndV3
 */
public class UcTokenVO {

    /** 用户 uid（迁移兑换响应返回；刷新响应不含此字段，为 null；下发前端前会被清空） */
    @JsonProperty("uid")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long uid;

    /** 访问令牌，前端调 UC 接口时携带 */
    @JsonProperty("access_token")
    private String accessToken;

    /** 令牌类型，固定 Bearer */
    @JsonProperty("token_type")
    private String tokenType;

    /** access_token 有效期（秒） */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /** 刷新令牌，仅服务端持有，下发浏览器前必须剥离 */
    @JsonProperty("refresh_token")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;

    /** 授权范围（逗号分隔） */
    @JsonProperty("scope")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String scope;

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

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
