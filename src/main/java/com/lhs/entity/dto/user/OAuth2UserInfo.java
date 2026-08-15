package com.lhs.entity.dto.user;

/**
 * OAuth2 用户信息响应
 * <p>
 * 对应 UC /oauth2/userinfo 接口 data 部分。
 * 基础字段为 uid/clientId/scope；userName/avatar/email 需 UC 扩展返回，缺失时使用本地兜底值
 */
public class OAuth2UserInfo {

    /** 用户 uid（本地资料缓存表主键） */
    private Long uid;

    /** 签发令牌的客户端 ID */
    private String clientId;

    /** 授权范围 */
    private String scope;

    /** 用户名（UC 扩展字段，可空） */
    private String userName;

    /** 头像（UC 扩展字段，可空） */
    private String avatar;

    /** 邮箱（UC 扩展字段，可空） */
    private String email;

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
