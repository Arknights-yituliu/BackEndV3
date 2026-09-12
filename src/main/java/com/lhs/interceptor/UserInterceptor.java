package com.lhs.interceptor;

import com.lhs.common.context.UserContext;
import com.lhs.common.util.Logger;
import com.lhs.entity.po.user.OAuthUserInfo;
import com.lhs.service.user.OAuthUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class UserInterceptor implements HandlerInterceptor {

    private final OAuthUserService oAuthUserService;

    public UserInterceptor(OAuthUserService oAuthUserService) {
        this.oAuthUserService = oAuthUserService;
    }

    /**
     * 目标方法执行之前
     * 登录检查写在这里，如果没有登录，就不执行目标方法
     *
     * @param request  请求
     * @param response 响应
     * @param handler  操作
     * @return 登录状态
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 跨域预检请求直接放行，不做鉴权
        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }

        // 解析 token，无合法 token 时抛 USER_NOT_LOGIN（由全局异常处理器兜底）
        String token = oAuthUserService.extractToken(request);

        // 校验登录态并加载用户信息（Redis 校验 + 资料缓存表查询）
        OAuthUserInfo userInfo = oAuthUserService.getUserInfoPOByToken(token);

        // 将当前登录用户上下文写入线程级容器，供 Controller/Service 直接取用
        UserContext.set(userInfo.getId(), token, userInfo);

        return true;
    }

    /**
     * 目标方法执行完成以后
     *
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    /**
     * 页面渲染以后
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 请求结束后清理线程上下文，防止 ThreadLocal 内存泄漏
        UserContext.clear();
    }

}
