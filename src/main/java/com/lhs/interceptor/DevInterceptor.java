package com.lhs.interceptor;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.LogUtil;
import com.lhs.common.util.ResultCode;
import com.lhs.service.dev.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
public class DevInterceptor implements HandlerInterceptor {



    private final RedisTemplate<String, Object> redisTemplate;

    private final UserService userService;
    public DevInterceptor(RedisTemplate<String, Object> redisTemplate,UserService userService){
            this.redisTemplate =redisTemplate;
        this.userService =userService;
    }


    /**
     * 目标方法执行之前
     * 登录检查写在这里，如果没有登录，就不执行目标方法
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//      获取进过拦截器的路径
        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }

        String requestURI = request.getRequestURI();
        LogUtil.info("拦截路径："+requestURI);
        String token = request.getHeader("token");
        if(token ==null) throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        //  检查开发者Token

        return userService.loginAndCheckToken(token);
    }

    /**
     * 目标方法执行完成以后
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
