package com.lhs.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.exception.TclApiException;
import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import com.lhs.entity.dto.user.TclOperatorListVO;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * TCL PAT 消费客户端：目标账号由 PAT 的 payload 或历史默认来源决定。
 *
 * <p>与 {@link TclMigrateClient} 的分工：本类只负责「业务数据读取」，
 * 令牌的兑换、刷新与撤销仍由 TclMigrateClient 承担。两者都调用 TCL，
 * 但认证方式不同——本类用 Bearer PAT，
 * 迁移兑换用 Ed25519 签名 + IP 白名单。</p>
 *
 * <p>对应《PAT接口文档》第 7 节，需要 ak-data.read（上级 gama-data.read 可覆盖）。
 * PAT 不能刷新或用于 OAuth 资源接口；失效后需重新签发。</p>
 *
 * @author BackEndV3
 */
@Component
public class TclGameDataClient {

    /** 不附加 akUid 或 source；账号选择交由 TCL PAT 消费端执行。 */
    private static final String PAT_OPERATORS_PATH = "/pat/ak-accounts/operators";

    private static final ObjectMapper PAT_JSON_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** TCL 统一响应中的成功码 */
    private static final int TCL_SUCCESS_CODE = 200;

    /** TCL 错误码：PAT 缺失、无效或已过期，不尝试 OAuth 刷新。 */
    private static final int TCL_CODE_TOKEN_INVALID = 80001;

    /** 单次调用 TCL 的超时（秒）：业务数据读取比令牌兑换耗时更长，取值略大于迁移配置 */
    private static final long REQUEST_TIMEOUT_SECONDS = 5L;

    private final OAuth2Properties oauth2Properties;

    private final HttpClient httpClient;

    public TclGameDataClient(OAuth2Properties oauth2Properties) {
        this.oauth2Properties = oauth2Properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /** 读取 PAT 指定账号的干员数据；旧 64 位令牌由 TCL 按历史默认来源处理。 */
    public TclOperatorListVO listPatOperators(String patToken) {
        String body = sendGet(PAT_OPERATORS_PATH, patToken);
        try {
            Result<TclOperatorListVO> result = PAT_JSON_MAPPER.readValue(body,
                    new TypeReference<Result<TclOperatorListVO>>() {});
            return requireSuccess(result, "TCL PAT 干员数据读取");
        } catch (JsonProcessingException e) {
            // 响应异常时不记录正文，避免泄露凭证或玩家数据。
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    /**
     * 发送带 Bearer PAT 的 GET 请求到 TCL 并返回响应体
     *
     * @param path     接口路径（不附加账号查询参数）
     * @param patToken PAT 明文
     * @return 响应体字符串
     * @throws ServiceException TCL 不可达或 HTTP 状态非 200
     */
    private String sendGet(String path, String patToken) {
        String url = trimTrailingSlash(oauth2Properties.getBaseUrl()) + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + patToken)
                .GET()
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Logger.warn("TCL 游戏数据请求失败，路径: {}，状态码: {}", path, response.statusCode());
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
            }
            return response.body();
        } catch (ServiceException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        } catch (Exception e) {
            Logger.warn("TCL 游戏数据请求异常，路径: {}，原因: {}", path, e.getMessage());
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    /**
     * 校验 TCL 统一响应是否成功，失败时按错误码分级记录日志并抛出异常
     *
     * @param result TCL 响应
     * @param scene  场景描述（用于日志）
     * @return 响应中的业务数据
     * @throws TclApiException   TCL 返回业务错误码
     * @throws ServiceException 响应结构异常
     */
    private static <T> T requireSuccess(Result<T> result, String scene) {
        if (result == null || result.getCode() == null) {
            Logger.warn("{}失败：TCL 响应无法解析", scene);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        if (result.getCode() == TCL_SUCCESS_CODE) {
            return result.getData();
        }
        int code = result.getCode();
        String desc = describeTclCode(code);
        if (code == TCL_CODE_TOKEN_INVALID) {
            Logger.info("{}失败，TCL 错误码 {}（{}）", scene, code, desc);
        } else {
            Logger.warn("{}失败，TCL 错误码 {}（{}）", scene, code, desc);
        }
        throw new TclApiException(code, desc);
    }

    /**
     * TCL PAT 消费相关错误码含义描述（依据《PAT接口文档》）
     *
     * @param code TCL 错误码
     * @return 含义描述
     */
    private static String describeTclCode(int code) {
        return switch (code) {
            case 80001 -> "PAT 缺失、无效或已过期";
            case 80008 -> "PAT 缺少 ak-data.read 授权范围，或游戏账号未绑定当前用户";
            case 10002 -> "PAT 未指定有效的游戏账号";
            case 40001 -> "TCL 服务端内部错误";
            default -> "TCL 返回未知错误码";
        };
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
