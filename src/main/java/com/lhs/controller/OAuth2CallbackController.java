package com.lhs.controller;

import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.OAuth2TokenResponse;
import com.lhs.entity.dto.user.OAuth2UserInfo;
import com.lhs.service.user.OAuth2ClientService;
import com.lhs.service.user.OAuthUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * 统一用户中心 OAuth2 授权回调接口
 * <p>
 * 用户在 UC 完成授权后由 UC 302 回跳到本接口（携带 code 与 state），
 * 本接口校验 state、换取令牌、获取用户信息并建立本地会话
 */
@RestController
@Tag(name = "OAuth2 授权回调")
public class OAuth2CallbackController {

    private final OAuth2ClientService oAuth2ClientService;
    private final OAuthUserService oAuthUserService;
    private final OAuth2Properties oAuth2Properties;

    public OAuth2CallbackController(OAuth2ClientService oAuth2ClientService, OAuthUserService oAuthUserService,
            OAuth2Properties oAuth2Properties) {
        this.oAuth2ClientService = oAuth2ClientService;
        this.oAuthUserService = oAuthUserService;
        this.oAuth2Properties = oAuth2Properties;
    }

    /**
     * 处理 UC 授权回调：校验 state → 授权码换令牌 → 获取用户信息 → 建立本地会话
     *
     * @param code     授权码
     * @param state    发起授权时生成的 state（防 CSRF）
     * @param response HTTP 响应对象（配置前端回跳地址时使用）
     * @return 本地会话（含 token）；配置了 frontend-redirect-url 时 302 跳转携带 token
     */
    @Operation(summary = "OAuth2 授权回调")
    @GetMapping("/oauth/callback")
    public Result<HashMap<String, Object>> callback(@RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response) {
        // 1. 校验 state（防 CSRF）并取出 code_verifier
        String codeVerifier = oAuth2ClientService.consumeCodeVerifier(state);

        // 2. 授权码换令牌
        OAuth2TokenResponse tokenResponse = oAuth2ClientService.exchangeToken(code, codeVerifier);

        // 3. 获取用户信息
        OAuth2UserInfo oAuth2UserInfo = oAuth2ClientService.getUserInfo(tokenResponse.getAccessToken());

        // 4. 以 UC uid 建立本地会话（资料缓存 upsert + 生成本地 Token）
        HashMap<String, Object> session = oAuthUserService.createSessionByOAuth2Uid(oAuth2UserInfo);

        // 5. 配置了前端回跳地址则 302 跳转携带 token，否则直接返回 JSON
        String frontendUrl = oAuth2Properties.getFrontendRedirectUrl();
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            try {
                String token = String.valueOf(session.get("token"));
                String redirectUrl = frontendUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
                response.sendRedirect(redirectUrl);
                return null;
            } catch (Exception e) {
                Logger.error("OAuth2 回调跳转前端失败", e);
            }
        }
        return Result.success(session);
    }
}
