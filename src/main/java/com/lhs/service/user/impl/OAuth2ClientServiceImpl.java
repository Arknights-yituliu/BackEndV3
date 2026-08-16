package com.lhs.service.user.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.PkceUtil;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.OAuth2TokenResponse;
import com.lhs.entity.dto.user.OAuth2UserInfo;
import com.lhs.service.user.OAuth2ClientService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统一用户中心 OAuth2 客户端服务实现
 * <p>
 * 负责与 UC 的 /oauth2/** 端点交互：authorize 跳转、授权码换令牌、刷新令牌、获取用户信息
 */
@Service
public class OAuth2ClientServiceImpl implements OAuth2ClientService {

    /** state 缓存 key 前缀 */
    private static final String STATE_KEY_PREFIX = "oauth2:auth:state:";

    /** state 有效期（秒），对齐授权码有效期 */
    private static final long STATE_TTL_SECONDS = 600;

    private final OAuth2Properties oauth2Properties;
    private final RedisTemplate<String, String> redisTemplate;
    private final HttpClient httpClient;

    public OAuth2ClientServiceImpl(OAuth2Properties oauth2Properties, RedisTemplate<String, String> redisTemplate) {
        this.oauth2Properties = oauth2Properties;
        this.redisTemplate = redisTemplate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public Map<String, String> createAuthorizationSession() {
        // 生成 state 与 PKCE 参数
        String state = PkceUtil.generateCodeVerifier();
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge;
        try {
            codeChallenge = PkceUtil.s256(codeVerifier);
        } catch (Exception e) {
            Logger.error("PKCE code_challenge 生成失败", e);
            throw new ServiceException(ResultCode.SYSTEM_INNER_ERROR);
        }
        // 缓存 code_verifier，供回调时校验 state 后取出
        redisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, codeVerifier, STATE_TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, String> session = new HashMap<>();
        session.put("state", state);
        session.put("codeChallenge", codeChallenge);
        return session;
    }

    @Override
    public String buildAuthorizeUrl(String state, String codeChallenge) {
        String base = trimTrailingSlash(oauth2Properties.getBaseUrl());
        return base + "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + encode(oauth2Properties.getClientId())
                + "&redirect_uri=" + encode(oauth2Properties.getRedirectUri())
                + "&scope=" + encode(oauth2Properties.getScope())
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256";
    }

    @Override
    public String consumeCodeVerifier(String state) {
        // state 不存在或已过期则拒绝（防 CSRF）
        String codeVerifier = redisTemplate.opsForValue().get(STATE_KEY_PREFIX + state);
        if (codeVerifier == null || codeVerifier.isEmpty()) {
            throw new ServiceException(ResultCode.USER_PERMISSION_NO_ACCESS_OR_TIME_OUT);
        }
        // 一次性使用，取后即删
        redisTemplate.delete(STATE_KEY_PREFIX + state);
        return codeVerifier;
    }

    @Override
    public OAuth2TokenResponse exchangeToken(String code, String codeVerifier) {
        String form = "grant_type=authorization_code"
                + "&client_id=" + encode(oauth2Properties.getClientId())
                + "&client_secret=" + encode(oauth2Properties.getClientSecret())
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(oauth2Properties.getRedirectUri())
                + "&code_verifier=" + encode(codeVerifier);
        return postTokenForm(form);
    }

    @Override
    public OAuth2TokenResponse refreshToken(String refreshToken) {
        String form = "grant_type=refresh_token"
                + "&client_id=" + encode(oauth2Properties.getClientId())
                + "&client_secret=" + encode(oauth2Properties.getClientSecret())
                + "&refresh_token=" + encode(refreshToken);
        return postTokenForm(form);
    }

    @Override
    public OAuth2UserInfo getUserInfo(String accessToken) {
        String url = trimTrailingSlash(oauth2Properties.getBaseUrl()) + "/oauth2/userinfo";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        String body = send(request);
        Result<OAuth2UserInfo> result = JsonMapper.parseObject(body, new TypeReference<Result<OAuth2UserInfo>>() {
        });
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            Logger.error("UC userinfo 调用失败：{}" + body);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return result.getData();
    }

    /**
     * 发送表单 POST 请求换取令牌（/oauth2/token）
     *
     * @param form form-urlencoded 请求体
     * @return 令牌响应
     */
    private OAuth2TokenResponse postTokenForm(String form) {
        String url = trimTrailingSlash(oauth2Properties.getBaseUrl()) + "/oauth2/token";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .timeout(Duration.ofSeconds(10))
                .build();

        String body = send(request);

        Result<OAuth2TokenResponse> result = JsonMapper.parseObject(body, new TypeReference<Result<OAuth2TokenResponse>>() {
        });
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            Logger.error("UC token 交换失败：{}" + body);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return result.getData();
    }

    /**
     * 发送 HTTP 请求并返回响应体
     *
     * @param request 请求对象
     * @return 响应体字符串
     */
    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Logger.error("UC 请求失败，状态码：{}" + response.statusCode() + "，响应：{}" + response.body());
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
            }
            return response.body();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            Logger.error("UC 请求异常", e);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    /**
     * URL 编码工具方法
     *
     * @param value 待编码字符串
     * @return 编码结果
     */
    private String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 去除 baseUrl 末尾的斜杠，避免拼接出双斜杠
     *
     * @param baseUrl 服务根地址
     * @return 去掉末尾斜杠的地址
     */
    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "";
        }
        String result = baseUrl;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
