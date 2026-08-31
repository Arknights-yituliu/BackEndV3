package com.lhs.interceptor;

import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Logger;
import com.lhs.service.admin.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
public class AdminInterceptor implements HandlerInterceptor {

    private final AdminService adminService;

    public AdminInterceptor(AdminService adminService) {
        this.adminService = adminService;
    }


    /**
     * 目标方法执行之前
     * 管理员/开发者鉴权：校验请求携带的开发者 token，非管理员或无有效 token 则拒绝执行目标方法
     *
     * @param request  请求
     * @param response 响应
     * @param handler  操作
     * @return 是否放行
     * @throws Exception 校验失败时抛出 ServiceException
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            return true;
        }

        // 校验开发者身份：token 缺失/无效/过期由 developerLevel 内部抛异常，非管理员返回 false 拒绝
        Boolean isDeveloper = adminService.developerLevel(request);
        if (isDeveloper == null || !isDeveloper) {
            Logger.info("管理员鉴权失败，拒绝访问：{}", request.getRequestURI());
            throw new ServiceException(ResultCode.USER_INSUFFICIENT_PERMISSIONS);
        }
        return true;
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
