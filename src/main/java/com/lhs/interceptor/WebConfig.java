package com.lhs.interceptor;

import com.lhs.service.dev.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserService userService;
    /**
     * 配置拦截器
     * @param registry 拦截器的注册中心
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new DevInterceptor(redisTemplate,userService))
                .addPathPatterns("/auth/**"); //拦截
//                .excludePathPatterns(); //放行

        registry.addInterceptor(new SurveyInterceptor(redisTemplate))
                .addPathPatterns("/survey/register/**"); //拦截
//                .excludePathPatterns(); //放行

    }
}
