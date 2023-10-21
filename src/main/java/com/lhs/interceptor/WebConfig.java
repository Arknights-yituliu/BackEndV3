package com.lhs.interceptor;

import com.lhs.service.util.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    private final RedisTemplate<String, Object> redisTemplate;

    private final UserService userService;

    public WebConfig(RedisTemplate<String, Object> redisTemplate, UserService userService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

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
