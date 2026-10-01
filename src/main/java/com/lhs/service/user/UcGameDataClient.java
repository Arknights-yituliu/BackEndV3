package com.lhs.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.exception.UcApiException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.UcAkAccountVO;
import com.lhs.entity.dto.user.UcOperatorListVO;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * UC 游戏数据客户端：读取用户已绑定的游戏账号与干员数据
 *
 * <p>与 {@link UcMigrateClient} 的分工：本类只负责「业务数据读取」，
 * 令牌的兑换、刷新与撤销仍由 UcMigrateClient 承担。两者都调用 UC，
 * 但认证方式不同——本类用用户维度的 Bearer access_token，
 * 迁移兑换用 Ed25519 签名 + IP 白名单。</p>
 *
 * <p>对应 UC 文档《OAuth 游戏数据接口》第 2、3 节，需要 gama-data.read 授权范围。
 * access_token 失效时 UC 返回业务码 80001（HTTP 状态仍为 200），
 * 调用方应刷新令牌后重试一次。</p>
 *
 * @author BackEndV3
 */
@Component
public class UcGameDataClient {

    /** 已绑定游戏账号列表接口路径 */
    private static final String AK_ACCOUNTS_PATH = "/oauth2/ak-accounts";

    /** 干员数据全量读取接口路径 */
    private static final String AK_OPERATORS_PATH = "/oauth2/ak-accounts/operators";

    /** UC 统一响应中的成功码 */
    private static final int UC_SUCCESS_CODE = 200;

    /** UC 错误码：access_token 缺失、无效或已过期，需刷新令牌后重试 */
    public static final int UC_CODE_TOKEN_INVALID = 80001;

    /** 单次调用 UC 的超时（秒）：业务数据读取比令牌兑换耗时更长，取值略大于迁移配置 */
    private static final long REQUEST_TIMEOUT_SECONDS = 5L;

    private final OAuth2Properties oauth2Properties;

    private final HttpClient httpClient;

    public UcGameDataClient(OAuth2Properties oauth2Properties) {
        this.oauth2Properties = oauth2Properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * 查询用户已绑定的游戏账号列表
     *
     * @param accessToken 用户维度的 UC access_token
     * @return 已绑定游戏账号列表（UC 侧按更新时间倒序返回）；无绑定时为空列表
     * @throws UcApiException   UC 返回业务错误码（80001 表示令牌失效）
     * @throws ServiceException UC 不可达或响应无法解析
     */
    public List<UcAkAccountVO> listAkAccounts(String accessToken) {
        String body = sendGet(AK_ACCOUNTS_PATH, accessToken);
        Result<List<UcAkAccountVO>> result = JsonMapper.parseObject(body,
                new TypeReference<Result<List<UcAkAccountVO>>>() {
                });
        return requireSuccess(result, "UC 游戏账号列表查询");
    }

    /**
     * 全量读取指定游戏账号的干员数据
     *
     * @param accessToken 用户维度的 UC access_token
     * @param akUid       游戏账号 UID
     * @return 干员数据（含 akUid 与 items）；无数据时 items 为空数组
     * @throws UcApiException   UC 返回业务错误码（80001 令牌失效，80008 账号不在该用户名下）
     * @throws ServiceException UC 不可达或响应无法解析
     */
    public UcOperatorListVO listOperators(String accessToken, String akUid) {
        String path = AK_OPERATORS_PATH + "?akUid=" + encode(akUid);
        String body = sendGet(path, accessToken);
        Result<UcOperatorListVO> result = JsonMapper.parseObject(body,
                new TypeReference<Result<UcOperatorListVO>>() {
                });
        return requireSuccess(result, "UC 干员数据读取");
    }

    /**
     * 发送带 Bearer 令牌的 GET 请求到 UC 并返回响应体
     *
     * @param path        接口路径（可含查询串，以 / 开头）
     * @param accessToken UC access_token
     * @return 响应体字符串
     * @throws ServiceException UC 不可达或 HTTP 状态非 200
     */
    private String sendGet(String path, String accessToken) {
        String url = trimTrailingSlash(oauth2Properties.getBaseUrl()) + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + (accessToken == null ? "" : accessToken))
                .GET()
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Logger.warn("UC 游戏数据请求失败，路径: {}，状态码: {}", path, response.statusCode());
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
            }
            return response.body();
        } catch (ServiceException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        } catch (Exception e) {
            Logger.warn("UC 游戏数据请求异常，路径: {}，原因: {}", path, e.getMessage());
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    /**
     * 校验 UC 统一响应是否成功，失败时按错误码分级记录日志并抛出异常
     *
     * @param result UC 响应
     * @param scene  场景描述（用于日志）
     * @return 响应中的业务数据
     * @throws UcApiException   UC 返回业务错误码
     * @throws ServiceException 响应结构异常
     */
    private static <T> T requireSuccess(Result<T> result, String scene) {
        if (result == null || result.getCode() == null) {
            Logger.warn("{}失败：UC 响应无法解析", scene);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        if (result.getCode() == UC_SUCCESS_CODE) {
            return result.getData();
        }
        int code = result.getCode();
        String desc = describeUcCode(code);
        if (code == UC_CODE_TOKEN_INVALID) {
            // 令牌失效属可自愈状态：由调用方刷新令牌后重试，只记 info 避免告警噪音
            Logger.info("{}失败，UC 错误码 {}（{}）", scene, code, desc);
        } else {
            Logger.warn("{}失败，UC 错误码 {}（{}），UC 描述: {}", scene, code, desc, result.getMsg());
        }
        throw new UcApiException(code, desc);
    }

    /**
     * UC 游戏数据相关错误码含义描述（依据《OAuth 游戏数据接口》第 5 节）
     *
     * @param code UC 错误码
     * @return 含义描述
     */
    private static String describeUcCode(int code) {
        return switch (code) {
            case 80001 -> "access_token 缺失、无效或已过期";
            case 80008 -> "缺少 gama-data.read 授权范围，或该游戏账号不在当前用户名下";
            case 10002 -> "请求参数校验失败";
            case 30006 -> "该游戏账号上传过于频繁";
            case 40003 -> "限流服务不可用";
            case 40004 -> "干员数据保存冲突";
            case 40001 -> "UC 服务端内部错误";
            default -> "UC 返回未知错误码";
        };
    }

    /**
     * URL 编码工具方法
     *
     * @param value 待编码字符串
     * @return 编码结果
     */
    private static String encode(String value) {
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
    private static String trimTrailingSlash(String baseUrl) {
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
