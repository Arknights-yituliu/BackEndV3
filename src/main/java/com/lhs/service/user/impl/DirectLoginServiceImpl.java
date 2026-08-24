package com.lhs.service.user.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.DirectLoginSessionVO;
import com.lhs.entity.dto.user.DirectLoginUserVO;
import com.lhs.entity.dto.user.OAuth2UserInfo;
import com.lhs.entity.vo.user.LoginSessionVO;
import com.lhs.service.user.DirectLoginService;
import com.lhs.service.user.OAuthUserService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * UC 直连登录接入服务实现（方案 B：保持旧系统登录页）
 * <p>
 * 调用 UC /oauth2/direct-session 发起会话、/oauth2/direct-user 兑换用户信息，
 * 兑换成功后复用 OAuthUserService 建立本地会话（资料缓存 upsert + 生成本地 Token）
 */
@Service
public class DirectLoginServiceImpl implements DirectLoginService {

    private final OAuth2Properties oauth2Properties;
    private final OAuthUserService oAuthUserService;
    private final HttpClient httpClient;

    public DirectLoginServiceImpl(OAuth2Properties oauth2Properties, OAuthUserService oAuthUserService) {
        this.oauth2Properties = oauth2Properties;
        this.oAuthUserService = oAuthUserService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public DirectLoginSessionVO createDirectSession() {
        String form = "client_id=" + encode(oauth2Properties.getClientId())
                + "&client_secret=" + encode(oauth2Properties.getClientSecret());
        String body = sendForm("/oauth2/direct-session", form);
        Result<DirectLoginSessionVO> result = JsonMapper.parseObject(body,
                new TypeReference<Result<DirectLoginSessionVO>>() {
                });
        if (result == null || result.getCode() == null || result.getCode() != 200 || result.getData() == null) {
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return result.getData();
    }

    @Override
    public LoginSessionVO completeLogin(String ticket) {
        if (!checkParamsValidity(ticket)) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }
        String form = "client_id=" + encode(oauth2Properties.getClientId())
                + "&client_secret=" + encode(oauth2Properties.getClientSecret())
                + "&ticket=" + encode(ticket);
        String body = sendForm("/oauth2/direct-user", form);
        Result<DirectLoginUserVO> result = JsonMapper.parseObject(body,
                new TypeReference<Result<DirectLoginUserVO>>() {
                });
        if (result == null || result.getCode() == null || result.getCode() != 200 || result.getData() == null) {
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        DirectLoginUserVO ucUser = result.getData();
        // 账号被封禁则拒绝放行
        if (ucUser.getStatus() == null || ucUser.getStatus() < 1) {
            throw new ServiceException(ResultCode.USER_FORBIDDEN);
        }

        // 转为 OAuth2UserInfo 复用本地会话建立逻辑（资料缓存 + 生成本地 Token）
        OAuth2UserInfo oAuth2UserInfo = new OAuth2UserInfo();
        oAuth2UserInfo.setUid(ucUser.getUid());
        oAuth2UserInfo.setNickname(ucUser.getNickname());
        oAuth2UserInfo.setAvatar(ucUser.getAvatar());
        oAuth2UserInfo.setEmail(ucUser.getEmail());
        return oAuthUserService.createSessionByOAuth2Uid(oAuth2UserInfo);
    }

    /**
     * 发送表单 POST 请求到 UC 并返回响应体
     *
     * @param path UC 接口路径（以 / 开头，如 /oauth2/direct-session）
     * @param form form-urlencoded 请求体
     * @return 响应体字符串
     */
    private String sendForm(String path, String form) {
        String url = trimTrailingSlash(oauth2Properties.getBaseUrl()) + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .timeout(Duration.ofSeconds(10))
                .build();
        return send(request);
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
                Logger.error("UC 请求失败，状态码：{}" + response.statusCode());
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

    /**
     * 验证参数是否为空
     *
     * @param param 参数
     * @return 参数是否有效
     */
    private static Boolean checkParamsValidity(String param) {
        if (param == null) {
            return false;
        }
        if ("undefined".equals(param) || "null".equals(param)) {
            return false;
        }
        return !param.isEmpty();
    }
}
