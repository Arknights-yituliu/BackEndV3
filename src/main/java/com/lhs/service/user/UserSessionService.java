package com.lhs.service.user;

import com.lhs.entity.dto.user.DirectLoginUserVO;
import com.lhs.entity.po.user.OAuthUserInfo;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.entity.vo.user.LoginSessionVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户会话服务
 * <p>
 * 用户中心迁移后，本地不再自建账号。本服务承载所有与"当前登录用户"相关的逻辑：
 * 会话建立（UC 返回的用户信息落库资料缓存 + 生成 Token）、登录态校验、用户信息查询等。
 * 已随迁移下线的注册/密码/找回/改密等逻辑保留在 UserService，不再对外使用
 */
public interface UserSessionService {

    /**
     * 从请求中提取 token
     *
     * @param request HTTP 请求对象
     * @return token
     */
    String extractToken(HttpServletRequest request);

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
     * 用户登出，删除 Redis 中的登录 token
     *
     * @param httpServletRequest HTTP 请求对象
     */
    void logout(HttpServletRequest httpServletRequest);

    /**
     * 通过 UC 返回的用户信息建立本地会话（资料缓存 upsert + 生成本地 Token）
     * <p>
     * 以 UC uid 为准，仅维护本地资料缓存表，不保存任何身份/密码信息
     *
     * @param directLoginUserVO UC 返回的用户信息
     * @return 本地会话（含 token）
     */
    LoginSessionVO createSession(DirectLoginUserVO directLoginUserVO);


    /**
     * 修改当前登录用户的昵称
     *
     * @param httpServletRequest HTTP 请求对象
     * @param nickname           新昵称
     */
    void updateNickname(HttpServletRequest httpServletRequest, String nickname);

    /**
     * 修改当前登录用户的头像
     *
     * @param httpServletRequest HTTP 请求对象
     * @param avatar             新头像地址
     */
    void updateAvatar(HttpServletRequest httpServletRequest, String avatar);

    /**
     * 备份用户信息（资料缓存表）到腾讯云 COS
     */
    void backupUserInfo();
}
