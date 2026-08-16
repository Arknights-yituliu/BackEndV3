package com.lhs.service.user;

import com.lhs.entity.dto.user.OAuth2UserInfo;
import com.lhs.entity.po.user.OAuthUserInfo;
import com.lhs.entity.vo.survey.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;

/**
 * OAuth2 用户中心接入后的用户服务
 * <p>
 * 用户中心迁移后，本地不再自建账号。本服务承载所有与"当前登录用户"相关的逻辑：
 * 会话建立（OAuth2 授权落库资料缓存 + 生成 Token）、登录态校验、用户信息查询等。
 * 已随迁移下线的注册/密码/找回/改密等逻辑保留在 UserService，不再对外使用
 */
public interface OAuthUserService {

    /**
     * 从请求中提取 token
     *
     * @param request HTTP 请求对象
     * @return token
     */
    String extractToken(HttpServletRequest request);

    /**
     * 检查用户登录状态（请求头是否携带合法格式的 Authorization）
     *
     * @param httpServletRequest HTTP 请求对象
     * @return 是否登录
     */
    Boolean checkUserLoginStatus(HttpServletRequest httpServletRequest);

    /**
     * 通过 token 获取用户信息
     *
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    UserInfoVO getUserInfoVOByToken(String token);

    /**
     * 通过 token 获取用户数据内的信息
     *
     * @param token 用户登录后获得的凭证
     * @return 用户信息
     */
    OAuthUserInfo getUserInfoPOByToken(String token);

    /**
     * 通过 HttpServletRequest 获取 token，根据 token 拿到用户信息
     *
     * @param httpServletRequest HTTP 请求对象
     * @return 用户信息
     */
    UserInfoVO getUserInfoVOByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 通过 HttpServletRequest 获取 token，根据 token 拿到用户 id；
     * 无 token 时优先取请求头 uid，仍没有则根据 IP 生成临时 uid
     *
     * @param httpServletRequest HTTP 请求对象
     * @return 用户 id
     */
    Long getUidByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 通过 HttpServletRequest 获取 token，根据 token 拿到用户信息
     *
     * @param httpServletRequest HTTP 请求对象
     * @return 用户信息
     */
    OAuthUserInfo getUserInfoPOByHttpServletRequest(HttpServletRequest httpServletRequest);

    /**
     * 用户登出，删除 Redis 中的登录 token
     *
     * @param httpServletRequest HTTP 请求对象
     */
    void logout(HttpServletRequest httpServletRequest);

    /**
     * 通过 OAuth2 授权获取的用户信息建立本地会话（资料缓存 upsert + 生成本地 Token）
     * <p>
     * 以 UC uid 为准，仅维护本地资料缓存表，不保存任何身份/密码信息
     *
     * @param oAuth2UserInfo UC 返回的用户信息
     * @return 本地会话（含 token）
     */
    HashMap<String, Object> createSessionByOAuth2Uid(OAuth2UserInfo oAuth2UserInfo);

    /**
     * 备份用户信息（资料缓存表）到腾讯云 COS
     */
    void backupUserInfo();
}
