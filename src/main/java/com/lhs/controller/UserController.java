package com.lhs.controller;

import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.service.user.OAuth2ClientService;
import com.lhs.service.user.OAuthUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 一图流用户系统控制器
 * <p>
 * 用户中心迁移后：注册/登录/找回/改密/邮箱绑定等接口已下线，
 * 登录统一走 OAuth2 授权（见 /user/oauth2/login 与 /oauth/callback）
 */
@RestController
@Tag(name = "一图流用户系统")
public class UserController {

    private final OAuthUserService oAuthUserService;
    private final OAuth2ClientService oAuth2ClientService;

    public UserController(OAuthUserService oAuthUserService, OAuth2ClientService oAuth2ClientService) {
        this.oAuthUserService = oAuthUserService;
        this.oAuth2ClientService = oAuth2ClientService;
    }

    @Operation(summary = "OAuth2 登录引导，返回 UC 授权跳转地址")
    @GetMapping("/user/oauth2/login")
    public Result<HashMap<String, Object>> oauth2Login() {
        Logger.info("【OAuth2 登录】发起授权引导，生成 state + PKCE 参数");
        // 生成 state + PKCE 参数并缓存 code_verifier
        Map<String, String> authSession = oAuth2ClientService.createAuthorizationSession();
        // 构造 UC 授权地址
        String authorizeUrl = oAuth2ClientService.buildAuthorizeUrl(
                authSession.get("state"), authSession.get("codeChallenge"));
        Logger.info("【OAuth2 登录】授权引导生成完成，state=" + authSession.get("state")
                + ", authorizeUrl=" + authorizeUrl);
        HashMap<String, Object> result = new HashMap<>();
        result.put("authorizeUrl", authorizeUrl);
        return Result.success(result);
    }

    @Operation(summary = "根据token检查用户登录状态吗，返回用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoVO> getUserInfo(@RequestParam String token) {
        UserInfoVO response = oAuthUserService.getUserInfoVOByToken(token);
        return Result.success(response);
    }

    @Operation(summary = "用户登出，使当前登录token失效")
    @PostMapping("/auth/user/logout")
    public Result<Object> logout(HttpServletRequest httpServletRequest) {
        oAuthUserService.logout(httpServletRequest);
        return Result.success();
    }

    @Operation(summary = "修改当前登录用户的昵称")
    @PostMapping("/auth/user/nickname")
    public Result<Object> updateNickname(HttpServletRequest httpServletRequest,
            @RequestBody Map<String, String> body) {
        oAuthUserService.updateNickname(httpServletRequest, body.get("nickname"));
        return Result.success();
    }

    @Operation(summary = "修改当前登录用户的头像")
    @PostMapping("/auth/user/avatar")
    public Result<Object> updateAvatar(HttpServletRequest httpServletRequest,
            @RequestBody Map<String, String> body) {
        oAuthUserService.updateAvatar(httpServletRequest, body.get("avatar"));
        return Result.success();
    }

}
