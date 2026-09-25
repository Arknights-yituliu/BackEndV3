package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.service.user.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 一图流用户系统控制器
 * <p>
 * 用户中心迁移后：注册/登录/找回/改密/邮箱绑定等接口已下线，
 * 登录统一走直连登录（见 /direct-session 与 /complete-login）
 */
@RestController
@Tag(name = "一图流用户系统")
public class UserController {

    private final UserSessionService userSessionService;

    public UserController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @Operation(summary = "根据token检查用户登录状态吗，返回用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoVO> getUserInfo(@RequestParam String token) {
        UserInfoVO response = userSessionService.getUserInfoVOByToken(token);
        return Result.success(response);
    }

    @Operation(summary = "用户登出，使当前登录token失效")
    @PostMapping("/auth/user/logout")
    public Result<Object> logout(HttpServletRequest httpServletRequest) {
        userSessionService.logout(httpServletRequest);
        return Result.success();
    }

    @Operation(summary = "修改当前登录用户的昵称")
    @PostMapping("/auth/user/nickname")
    public Result<Object> updateNickname(HttpServletRequest httpServletRequest,
            @RequestBody Map<String, String> body) {
        userSessionService.updateNickname(httpServletRequest, body.get("nickname"));
        return Result.success();
    }

    @Operation(summary = "修改当前登录用户的头像")
    @PostMapping("/auth/user/avatar")
    public Result<Object> updateAvatar(HttpServletRequest httpServletRequest,
            @RequestBody Map<String, String> body) {
        userSessionService.updateAvatar(httpServletRequest, body.get("avatar"));
        return Result.success();
    }

}
