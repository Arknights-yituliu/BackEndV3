package com.lhs.interceptor;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.IpUtil;
import com.lhs.common.util.ResultCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

public class SurveyInterceptor implements HandlerInterceptor {


    private final RedisTemplate<String, Object> redisTemplate;


    public SurveyInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate  = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String ipAddress = AES.encrypt(IpUtil.getIpAddress(request), ConfigUtil.Secret);
        Object cache = redisTemplate.opsForValue().get("IP:"+ipAddress);
        if(cache==null) {
            redisTemplate.opsForValue().set("IP:"+ipAddress, 1, 5, TimeUnit.MINUTES);
            return  true;
        }
        int times = Integer.parseInt(String.valueOf(cache));
        if(times>5) throw new ServiceException(ResultCode.EXCESSIVE_IP_ACCESS_TIMES);
        redisTemplate.opsForValue().increment("IP:"+ipAddress);
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
