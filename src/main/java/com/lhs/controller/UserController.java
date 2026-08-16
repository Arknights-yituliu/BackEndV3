package com.lhs.controller;

import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.OpenApiPermission;
import com.lhs.entity.dto.user.OpenApiTokenRequestDTO;
import com.lhs.entity.vo.survey.UserInfoVO;
import com.lhs.service.user.OAuth2ClientService;
import com.lhs.service.user.OAuthUserService;
import com.lhs.service.user.OpenApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    private final OpenApiService openApiService;
    private final OAuth2ClientService oAuth2ClientService;

    public UserController(OAuthUserService oAuthUserService, OpenApiService openApiService,
            OAuth2ClientService oAuth2ClientService) {
        this.oAuthUserService = oAuthUserService;
        this.openApiService = openApiService;
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

    @Operation(summary = "根据token检查用户登录状态吗，返回用户信息")
    @GetMapping("/user/info/v2")
    public Result<UserInfoVO> getUserInfoV2(HttpServletRequest httpServletRequest) {
        UserInfoVO response = oAuthUserService.getUserInfoVOByHttpServletRequest(httpServletRequest);
        return Result.success(response);
    }

    @Operation(summary = "生成第三方API Token，scope参数传入权限code数组，如[10001]")
    @PostMapping("/user/open-api/token")
    public Result<HashMap<String, Object>> generateOpenApiToken(HttpServletRequest httpServletRequest,
            @RequestBody OpenApiTokenRequestDTO request) {
        String token = openApiService.generateOpenApiToken(httpServletRequest, request.getScope(), request.getRemark());
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("scope", request.getScope());
        return Result.success(result);
    }

    @Operation(summary = "获取所有可用的 OpenAPI 权限列表")
    @GetMapping("/user/open-api/permissions")
    public Result<List<Map<String, Object>>> listPermissions() {
        return Result.success(OpenApiPermission.listAll());
    }

    @Operation(summary = "用户登出，使当前登录token失效")
    @PostMapping("/auth/user/logout")
    public Result<Object> logout(HttpServletRequest httpServletRequest) {
        oAuthUserService.logout(httpServletRequest);
        return Result.success();
    }

    @Operation(summary = "删除第三方API Token")
    @PostMapping("/auth/user/open-api/token/delete")
    public Result<Object> deleteOpenApiToken(HttpServletRequest httpServletRequest,
            @RequestBody Map<String, String> body) {
        openApiService.deleteOpenApiToken(httpServletRequest, body.get("token"));
        return Result.success();
    }

    @Operation(summary = "获取当前用户所有第三方API Token")
    @GetMapping("/auth/user/open-api/tokens")
    public Result<List<Map<String, Object>>> listOpenApiTokens(HttpServletRequest httpServletRequest) {
        return Result.success(openApiService.listUserTokens(httpServletRequest));
    }

}
