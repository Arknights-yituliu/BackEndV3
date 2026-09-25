package com.lhs.interceptor;

import com.lhs.service.admin.AdminService;
import com.lhs.service.user.UserSessionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    private final RedisTemplate<String, Object> redisTemplate;

    private final UserSessionService userSessionService;

    private final AdminService adminService;

    public WebConfig(RedisTemplate<String, Object> redisTemplate, UserSessionService userSessionService,
                     AdminService adminService) {
        this.redisTemplate = redisTemplate;
        this.userSessionService = userSessionService;
        this.adminService = adminService;
    }

    /**
     * 配置拦截器
     * @param registry 拦截器的注册中心
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new AdminInterceptor(adminService))
                .addPathPatterns("/admin/**"); //拦截
//                .excludePathPatterns(); //放行

        registry.addInterceptor(new SurveyInterceptor(redisTemplate))
                .addPathPatterns("/survey/register/**"); //拦截
//                .excludePathPatterns(); //放行


        registry.addInterceptor(new UserInterceptor(userSessionService))
                .addPathPatterns("/auth/**"); //拦截
//                .excludePathPatterns(); //放行

    }
}
