package com.lhs.entity.vo.user;

import lombok.Data;

/**
 * 登录会话 VO（本地登录凭证）
 * <p>
 * 由 UC 登录成功后的用户信息建立本地会话时返回：token 与 uid
 */
@Data
public class LoginSessionVO {

    /** 本地登录 token（后续请求携带于 Authorization 头） */
    private String token;

    /** 用户 uid（UC uid，本地资料缓存主键） */
    private Long uid;
}
