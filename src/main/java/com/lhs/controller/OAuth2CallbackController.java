package com.lhs.controller;

import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.OAuth2TokenResponse;
import com.lhs.entity.dto.user.OAuth2UserInfo;
import com.lhs.entity.vo.user.LoginSessionVO;
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
    public Result<LoginSessionVO> callback(@RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response) {
        Logger.info("【OAuth2 回调】进入回调接口，code=" + code + ", state=" + state);

        // 1. 校验 state（防 CSRF）并取出 code_verifier
        String codeVerifier = oAuth2ClientService.consumeCodeVerifier(state);
        Logger.info("【OAuth2 回调】校验 state 完成，codeVerifier 取到状态=" + (codeVerifier != null ? "成功" : "失败/null"));

        // 2. 授权码换令牌
        OAuth2TokenResponse tokenResponse = oAuth2ClientService.exchangeToken(code, codeVerifier);
        String accessToken = tokenResponse.getAccessToken();
        Logger.info("【OAuth2 回调】换取令牌成功，accessToken前16位=" + maskToken(accessToken)
                + ", 有效期=" + tokenResponse.getExpiresIn() + "秒, scope=" + tokenResponse.getScope());

        // 3. 获取用户信息
        OAuth2UserInfo oAuth2UserInfo = oAuth2ClientService.getUserInfo(accessToken);
        Logger.info("【OAuth2 回调】获取用户信息成功，uid=" + oAuth2UserInfo.getUid()
                + ", userName=" + oAuth2UserInfo.getUserName()
                + ", clientId=" + oAuth2UserInfo.getClientId()
                + ", email=" + oAuth2UserInfo.getEmail());

        // 4. 以 UC uid 建立本地会话（资料缓存 upsert + 生成本地 Token）
        LoginSessionVO session = oAuthUserService.createSessionByOAuth2Uid(oAuth2UserInfo);
        Logger.info("【OAuth2 回调】建立本地会话成功，本地token前16位=" + maskToken(session.getToken()));

        // 5. 配置了前端回跳地址则 302 跳转携带 token，否则直接返回 JSON
        String frontendUrl = oAuth2Properties.getFrontendRedirectUrl();
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            try {
                String token = session.getToken();
                String redirectUrl = frontendUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
                Logger.info("【OAuth2 回调】302 跳转前端，redirectUrl=" + redirectUrl);
                response.sendRedirect(redirectUrl);
                return null;
            } catch (Exception e) {
                Logger.error("OAuth2 回调跳转前端失败", e);
            }
        }
        Logger.info("【OAuth2 回调】未配置前端回跳地址，直接返回 JSON 会话");
        return Result.success(session);
    }

    /**
     * 脱敏 token，仅保留前 16 位，避免完整 token 落入日志
     *
     * @param token 待脱敏的 token
     * @return 脱敏后的 token 文本
     */
    private String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "null";
        }
        if (token.length() > 16) {
            return token.substring(0, 16) + "***";
        }
        return token + "***";
    }
}
