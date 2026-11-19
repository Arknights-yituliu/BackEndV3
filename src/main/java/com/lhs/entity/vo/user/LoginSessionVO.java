package com.lhs.entity.vo.user;

import lombok.Data;

/**
 * 登录会话 VO（本地登录凭证）
 * <p>
 * 由 TCL 登录成功后的用户信息建立本地会话时返回：token 与 uid；
 * 同时回带 TCL access_token 供前端调用 TCL 接口（refresh_token 留在服务端，
 * 前端刷新时带本地 token 调 /auth/token/refresh）
 */
@Data
public class LoginSessionVO {

    /** 本地登录 token（后续请求携带于 Authorization 头，兼作 TCL 令牌刷新凭据） */
    private String token;

    /** 用户 uid（TCL uid，本地资料缓存主键） */
    private Long uid;

    /** TCL access_token（前端调 TCL 接口用；TCL 未返回令牌时为空） */
    private String tclAccessToken;

    /** TCL access_token 有效期（秒） */
    private Long tclTokenExpiresIn;

    /** TCL 授权范围（逗号分隔） */
    private String tclTokenScope;
}
