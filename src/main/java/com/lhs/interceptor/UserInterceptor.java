package com.lhs.interceptor;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.LogUtils;
import com.lhs.common.enums.ResultCode;
import com.lhs.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


public class UserInterceptor implements HandlerInterceptor {


    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;

    public UserInterceptor(RedisTemplate<String, Object> redisTemplate, UserService userService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
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
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取进过拦截器的路径
        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }

        String requestURI = request.getRequestURI();
        LogUtils.info("一图流用户鉴权{}：");
        String token = userService.extractToken(request);
        //未能获取token则报错
        if (token == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
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
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
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
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

}
