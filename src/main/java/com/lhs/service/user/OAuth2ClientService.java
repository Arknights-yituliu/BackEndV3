package com.lhs.service.user;

import com.lhs.entity.dto.user.OAuth2TokenResponse;
import com.lhs.entity.dto.user.OAuth2UserInfo;

import java.util.Map;

/**
 * 统一用户中心 OAuth2 客户端服务
 */
public interface OAuth2ClientService {

    /**
     * 发起一次授权：生成 state 与 PKCE 参数，并缓存 code_verifier
     *
     * @return 包含 state、codeChallenge 的 Map
     */
    Map<String, String> createAuthorizationSession();

    /**
     * 构造浏览器跳转的授权地址（authorize URL）
     *
     * @param state         防 CSRF 随机串
     * @param codeChallenge PKCE code_challenge
     * @return authorize 完整 URL
     */
    String buildAuthorizeUrl(String state, String codeChallenge);

    /**
     * 校验回调 state 并取出对应的 code_verifier（一次性）
     *
     * @param state 回调带回的 state
     * @return code_verifier
     */
    String consumeCodeVerifier(String state);

    /**
     * 授权码换令牌
     *
     * @param code         回调地址带回的授权码
     * @param codeVerifier PKCE code_verifier
     * @return 令牌响应
     */
    OAuth2TokenResponse exchangeToken(String code, String codeVerifier);

    /**
     * 刷新令牌
     *
     * @param refreshToken 旧的刷新令牌
     * @return 新令牌对
     */
    OAuth2TokenResponse refreshToken(String refreshToken);

    /**
     * 获取授权用户信息
     *
     * @param accessToken access_token
     * @return 用户信息
     */
    OAuth2UserInfo getUserInfo(String accessToken);
}
