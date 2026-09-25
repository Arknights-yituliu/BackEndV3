package com.lhs.entity.vo.user;

import lombok.Data;

/**
 * 登录会话 VO（本地登录凭证）
 * <p>
 * 由 UC 登录成功后的用户信息建立本地会话时返回：token 与 uid；
 * 同时回带 UC access_token 供前端调用 UC 接口（refresh_token 留在服务端，
 * 前端刷新时带本地 token 调 /auth/token/refresh）
 */
@Data
public class LoginSessionVO {

    /** 本地登录 token（后续请求携带于 Authorization 头，兼作 UC 令牌刷新凭据） */
    private String token;

    /** 用户 uid（UC uid，本地资料缓存主键） */
    private Long uid;

    /** UC access_token（前端调 UC 接口用；UC 未返回令牌时为空） */
    private String ucAccessToken;

    /** UC access_token 有效期（秒） */
    private Long ucTokenExpiresIn;

    /** UC 授权范围（逗号分隔） */
    private String ucTokenScope;
}
