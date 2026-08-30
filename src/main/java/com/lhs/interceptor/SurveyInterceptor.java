package com.lhs.interceptor;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.common.util.RedisRateLimiter;
import com.lhs.common.enums.ResultCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SurveyInterceptor implements HandlerInterceptor {


    private final RedisTemplate<String, Object> redisTemplate;


    public SurveyInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate  = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String ipAddress = AES.encrypt(IpUtil.getIpAddress(request), ConfigUtil.Secret);
        // 60 秒窗口内同一 IP 最多访问 6 次，超限拒绝（与原有语义一致：第 7 次起拒绝）
        RedisRateLimiter.tryAcquire(redisTemplate, RedisKeyUtil.ipRate(ipAddress), 6, 60, ResultCode.EXCESSIVE_IP_ACCESS_TIMES);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
