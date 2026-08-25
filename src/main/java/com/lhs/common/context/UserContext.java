package com.lhs.common.context;

import com.lhs.entity.po.user.OAuthUserInfo;

/**
 * 当前登录用户上下文（线程级）
 * <p>
 * 由 UserInterceptor 在请求进入时写入、请求结束时清理，
 * 供 Controller/Service 直接获取当前登录用户的 uid、token 与用户信息，
 * 避免每个业务方法都从 HttpServletRequest 重复解析 token
 */
public class UserContext {

    /** 线程级上下文容器 */
    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    /** 当前登录用户 uid */
    private final Long uid;

    /** 当前登录用户 token（已去除 Authorization 前缀） */
    private final String token;

    /** 当前登录用户信息（资料缓存实体） */
    private final OAuthUserInfo userInfo;

    private UserContext(Long uid, String token, OAuthUserInfo userInfo) {
        this.uid = uid;
        this.token = token;
        this.userInfo = userInfo;
    }

    /**
     * 写入当前请求的用户上下文（由拦截器 preHandle 调用）
     *
     * @param uid      用户 uid
     * @param token    登录 token（已去前缀）
     * @param userInfo 用户信息
     */
    public static void set(Long uid, String token, OAuthUserInfo userInfo) {
        CONTEXT.set(new UserContext(uid, token, userInfo));
    }

    /**
     * 获取当前登录用户 uid；未登录或上下文已清理时返回 null
     *
     * @return 用户 uid
     */
    public static Long getUid() {
        UserContext context = CONTEXT.get();
        return context == null ? null : context.uid;
    }

    /**
     * 获取当前登录用户 token；未登录或上下文已清理时返回 null
     *
     * @return 登录 token
     */
    public static String getToken() {
        UserContext context = CONTEXT.get();
        return context == null ? null : context.token;
    }

    /**
     * 获取当前登录用户信息；未登录或上下文已清理时返回 null
     *
     * @return 用户信息实体
     */
    public static OAuthUserInfo getUserInfo() {
        UserContext context = CONTEXT.get();
        return context == null ? null : context.userInfo;
    }

    /**
     * 清理当前线程上下文（由拦截器 afterCompletion 调用，防止 ThreadLocal 内存泄漏）
     */
    public static void clear() {
        CONTEXT.remove();
    }
}