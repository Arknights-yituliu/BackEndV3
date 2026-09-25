package com.lhs.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.exception.UcApiException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.Result;
import com.lhs.common.util.SignUtil;
import com.lhs.entity.dto.user.UcTokenVO;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.UUID;

/**
 * UC 令牌客户端：迁移兑换、令牌刷新、令牌撤销
 *
 * <p>本类只负责「UC 令牌」相关的服务端间HTTP 调用，不参与登录与会话编排：</p>
 * <ul>
 *   <li>迁移兑换（文档 B3）：凭 uid 现场向 UC 换取一对全新 UC 令牌，Ed25519 签名 + IP 白名单认证；</li>
 *   <li>令牌刷新（文档 B12）：服务端代持 refresh_token 调 UC /oauth2/token，前端拿不到 refresh_token；</li>
 *   <li>令牌撤销（文档 R14 双撤）：登出时撤销 UC 侧授权，与本地删除 loginToken 同时进行。</li>
 * </ul>
 *
 * <p>迁移兑换端点跨公网可达，认证完全由 Ed25519 签名 + 时间窗 + nonce 承担，
 * 因此私钥只从配置注入，绝不写入日志。UC 业务错误码按 7.5 节建议分级处置：
 * 90016 / 80008 属配置问题立即告警，90015 属通道未上线只记 info，其余记 warn</p>
 *
 * @author BackEndV3
 */
@Component
public class UcMigrateClient {

    /** 迁移兑换端点路径：纳入签名原文，须与 UC 侧逐字节一致 */
    private static final String MIGRATE_PATH = "/oauth2/internal/migrate-token";

    /** 迁移兑换端点 HTTP 方法：纳入签名原文，防止签名被挪用到其他端点 */
    private static final String MIGRATE_METHOD = "POST";

    /** UC 统一响应中的成功码 */
    private static final int UC_SUCCESS_CODE = 200;

    /** UC 错误码：迁移兑换接口未开启（通道未上线，属预期状态，不告警） */
    private static final int UC_CODE_MIGRATE_DISABLED = 90015;

    /** UC 错误码：签名校验失败（配置或密钥错误，需立即告警） */
    private static final int UC_CODE_SIGN_INVALID = 90016;

    /** UC 错误码：IP 不在白名单（出口 IP 漂移或配置错误，需立即告警） */
    private static final int UC_CODE_IP_NOT_ALLOWED = 80008;

    /** UC 错误码：刷新令牌已失效或已被吊销（需清兑换缓存自愈） */
    public static final int UC_CODE_REFRESH_TOKEN_INVALID = 90009;

    private final OAuth2Properties oauth2Properties;

    private final HttpClient httpClient;

    /** 解析后的迁移签名私钥（配置在运行期不变，首次使用时解析并缓存） */
    private volatile PrivateKey migratePrivateKey;

    public UcMigrateClient(OAuth2Properties oauth2Properties) {
        this.oauth2Properties = oauth2Properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * 迁移兑换：凭自签 token 对应的 uid 换取一对全新 UC 令牌
     *
     * @param uid             自签 token 反查得到的 uid
     * @param legacyTokenHash 旧自签 token 的 sha256（仅作 UC 侧审计，可传 null）
     * @return UC 令牌对（access_token + refresh_token）
     * @throws UcApiException UC 返回业务错误码时抛出（携带原始错误码）
     */
    public UcTokenVO migrateToken(Long uid, String legacyTokenHash) {
        OAuth2Properties.Migrate migrate = oauth2Properties.getMigrate();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis() / 1000;
        String kid = migrate.getKid();
        String clientId = oauth2Properties.getClientId();

        // 签名原文须与 UC 侧逐字节一致：方法 + 路径 + kid + client_id + uid + ts + nonce
        String canonical = MIGRATE_METHOD + "\n" + MIGRATE_PATH + "\n"
                + kid + "\n" + clientId + "\n" + uid + "\n" + timestamp + "\n" + nonce;
        String sig = SignUtil.ed25519Sign(resolveMigratePrivateKey(), canonical);

        String form = "client_id=" + encode(clientId)
                + "&uid=" + encode(String.valueOf(uid))
                + "&ts=" + encode(String.valueOf(timestamp))
                + "&nonce=" + encode(nonce)
                + "&kid=" + encode(kid)
                + "&sig=" + encode(sig)
                + "&origin=" + encode(migrate.getOrigin())
                + "&legacy_token_hash=" + encode(legacyTokenHash == null ? "" : legacyTokenHash);

        String body = sendForm(MIGRATE_PATH, form);
        Result<UcTokenVO> result = JsonMapper.parseObject(body, new TypeReference<Result<UcTokenVO>>() {
        });
        requireSuccess(result, "UC 令牌兑换");
        UcTokenVO token = result.getData();
        if (token == null || token.getAccessToken() == null || token.getRefreshToken() == null) {
            // 令牌对不完整视为协议异常：缺 refresh_token 会导致后续无法刷新
            Logger.warn("UC 令牌兑换返回数据不完整，缺少 access_token 或 refresh_token");
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return token;
    }

    /**
     * 刷新 UC access_token（refresh_token 不轮换，调用方无需更新本地记录）
     *
     * @param refreshToken 服务端代持的刷新令牌
     * @return 新的 UC 令牌对（refresh_token 可能为空，表示未轮换）
     * @throws UcApiException UC 返回业务错误码时抛出（如 90009 刷新令牌已失效）
     */
    public UcTokenVO refreshToken(String refreshToken) {
        String form = "grant_type=" + encode("refresh_token")
                + "&client_id=" + encode(oauth2Properties.getClientId())
                + "&client_secret=" + encode(oauth2Properties.getClientSecret())
                + "&refresh_token=" + encode(refreshToken);
        String body = sendForm("/oauth2/token", form);
        Result<UcTokenVO> result = JsonMapper.parseObject(body, new TypeReference<Result<UcTokenVO>>() {
        });
        requireSuccess(result, "UC 令牌刷新");
        UcTokenVO token = result.getData();
        if (token == null || token.getAccessToken() == null) {
            Logger.warn("UC 令牌刷新返回数据不完整，缺少 access_token");
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        return token;
    }

    /**
     * 撤销 UC 令牌（登出双撤的另一半）
     *
     * <p>RFC 7009 语义：令牌不存在或已失效同样返回成功，因此本方法幂等。
     * 撤销失败只记录日志，不阻断本地登出——本地 loginToken 必须删掉，
     * 否则用户会处于「已登出但会话仍有效」的矛盾状态。</p>
     *
     * @param token 待撤销的令牌（access_token 或 refresh_token）
     */
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            String form = "client_id=" + encode(oauth2Properties.getClientId())
                    + "&client_secret=" + encode(oauth2Properties.getClientSecret())
                    + "&token=" + encode(token);
            sendForm("/oauth2/revoke", form);
        } catch (Exception e) {
            Logger.warn("撤销 UC 令牌失败（本地登出已完成，不影响会话注销）: {}", e.getMessage());
        }
    }

    /**
     * 校验 UC 统一响应是否成功，失败时按错误码分级记录日志并抛出异常
     *
     * @param result UC 响应
     * @param scene  场景描述（用于日志）
     * @throws UcApiException UC 返回业务错误码
     * @throws ServiceException 响应结构异常
     */
    private static void requireSuccess(Result<UcTokenVO> result, String scene) {
        if (result == null || result.getCode() == null) {
            Logger.warn("{}失败：UC 响应无法解析", scene);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        if (result.getCode() == UC_SUCCESS_CODE) {
            return;
        }
        int code = result.getCode();
        String desc = describeUcCode(code);
        if (code == UC_CODE_SIGN_INVALID || code == UC_CODE_IP_NOT_ALLOWED) {
            // 配置或密钥错误：重试无意义，立即告警
            Logger.error("{}失败，UC 错误码 {}（{}），属配置问题请立即排查", scene, code, desc);
        } else if (code == UC_CODE_MIGRATE_DISABLED) {
            // 通道未上线：属预期状态，只记 info，避免告警风暴
            Logger.info("{}未生效，UC 错误码 {}（{}）", scene, code, desc);
        } else {
            Logger.warn("{}失败，UC 错误码 {}（{}）", scene, code, desc);
        }
        throw new UcApiException(code, desc);
    }

    /**
     * UC 错误码含义描述（依据文档 7.5 节错误码对照表）
     *
     * @param code UC 错误码
     * @return 含义描述
     */
    private static String describeUcCode(int code) {
        return switch (code) {
            case 90015 -> "迁移兑换接口未开启";
            case 90016 -> "迁移请求签名校验失败";
            case 90017 -> "迁移请求已过期或已被使用";
            case 80008 -> "请求 IP 不在白名单";
            case 90009 -> "刷新令牌已失效或已被吊销";
            case 30005 -> "触发限流，请检查是否重复兑换";
            case 90001 -> "客户端不存在";
            case 90013 -> "客户端未审批";
            case 90014 -> "客户端未开通直连认证";
            case 20001 -> "用户不存在";
            case 20004 -> "用户已封禁";
            default -> "UC 返回未知错误码";
        };
    }

    /**
     * 发送表单 POST 请求到 UC 并返回响应体
     *
     * @param path UC 接口路径（以 / 开头）
     * @param form form-urlencoded 请求体
     * @return 响应体字符串
     */
    private String sendForm(String path, String form) {
        String url = trimTrailingSlash(oauth2Properties.getBaseUrl()) + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .timeout(Duration.ofSeconds(oauth2Properties.getMigrate().getTimeoutSeconds()))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Logger.warn("UC 请求失败，路径: {}，状态码: {}", path, response.statusCode());
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
            }
            return response.body();
        } catch (ServiceException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        } catch (Exception e) {
            Logger.warn("UC 请求异常，路径: {}，原因: {}", path, e.getMessage());
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }

    /**
     * 解析迁移签名私钥（懒加载 + 缓存，避免每次兑换都做一次 Base64 解码与密钥解析）
     *
     * @return Ed25519 私钥
     */
    private PrivateKey resolveMigratePrivateKey() {
        PrivateKey cached = migratePrivateKey;
        if (cached != null) {
            return cached;
        }
        PrivateKey parsed = SignUtil.parseEd25519PrivateKey(oauth2Properties.getMigrate().getPrivateKey());
        migratePrivateKey = parsed;
        return parsed;
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
