package com.lhs.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.exception.TclApiException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.common.util.RedisRateLimiter;
import com.lhs.entity.dto.user.TclTokenVO;
import com.lhs.entity.dto.user.PatIssueRequestDTO;
import com.lhs.entity.dto.user.TclPatVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TCL 令牌迁移服务：存量登录态按需兑换、刷新与撤销
 *
 * <p>自签 token 是 BackEndV3 的会话权威（鉴权只查本地 loginToken，不校验 TCL 令牌），
 * 本服务只负责为用户额外取得一份 TCL 授权，使前端可直连 TCL 接口：</p>
 * <ul>
 *   <li>按需兑换（B4）：兑换结果按 <b>uid 维度</b>缓存到 {@code uc:token:{uid}}，
 *       同一用户全程只兑换一次，既避免重复兑换击穿 TCL 侧 5/分钟限流，
 *       也对齐 TCL 侧「(uid, client_id) 上恒一条迁移凭证」的幂等模型——
 *       若按自签 token 分片，多设备会各兑换一次并互相撤销，旧设备刷新即掉登录；</li>
 *   <li>并发去重（B6）：进程内锁 + Redis 短锁，多标签页同时触发时只有一个真正调用 TCL；</li>
 *   <li>失败降级（B7）：兑换失败只让兑换接口返回失败，不影响业务请求与登录态
 *       （鉴权仍走本地 loginToken），前端下次进入业务页/启动时重试；连续失败达阈值告警；</li>
 *   <li>刷新与撤销（B12 / R14）：服务端代持 refresh_token 刷新；登出时与本地 loginToken 双撤。</li>
 * </ul>
 *
 * @author BackEndV3
 */
@Service
public class TclTokenMigrateService {

    /**
     * 兑换缓存有效期（天）：对齐 TCL refresh_token 的 90 天生命周期，
     * 可早于本地 loginToken（180 天）过期；到期后下次调用按需重新兑换
     */
    private static final long ISSUED_CACHE_TTL_DAYS = 90L;

    /** 并发去重短锁持有时长（秒）：只需覆盖一次兑换的往返时间 */
    private static final long LOCK_TTL_SECONDS = 10L;

    /** 连续兑换失败告警阈值：达到即打 error 日志，提示排查迁移端点与签名配置 */
    private static final int FAIL_ALERT_THRESHOLD = 10;

    /** PAT 文档约定的最长有效期：10 年零 1 个月。 */
    private static final long PAT_MAX_EXPIRES_SECONDS = 318038400L;

    /** PAT 名称最大长度。 */
    private static final int PAT_NAME_MAX_LENGTH = 50;

    /** PAT scope 文档上限。 */
    private static final int PAT_SCOPE_MAX_LENGTH = 256;

    /** PAT 签发单用户限流：窗口内允许的调用次数。 */
    private static final long PAT_ISSUE_MAX_PER_WINDOW = 3L;

    /** PAT 签发单用户限流窗口（秒）。 */
    private static final long PAT_ISSUE_WINDOW_SECONDS = 60L;

    private final OAuth2Properties oauth2Properties;
    private final TclMigrateClient tclMigrateClient;
    private final RedisTemplate<String, String> redisTemplate;

    /** 进程内按 uid 的兑换锁：与 Redis 短锁配合，避免单实例内多线程重复调用 TCL */
    private final ConcurrentHashMap<Long, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    /** 连续兑换失败次数（成功即清零），用于触发告警日志 */
    private final AtomicInteger continuousFailCount = new AtomicInteger();

    public TclTokenMigrateService(OAuth2Properties oauth2Properties,
                                 TclMigrateClient tclMigrateClient,
                                 RedisTemplate<String, String> redisTemplate) {
        this.oauth2Properties = oauth2Properties;
        this.tclMigrateClient = tclMigrateClient;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 兑换 TCL 令牌（前端主动调用的兑换接口使用）
     *
     * <p>缓存命中直接复用；未命中才签名调用 TCL。失败时抛出异常让接口返回失败，
     * 前端不阻塞业务（自签 token 仍可正常调 BackEndV3），下次进入业务页再重试</p>
     *
     * @param uid             当前登录用户 uid
     * @param legacyTokenHash 旧自签 token 的 sha256（仅作 TCL 侧审计）
     * @return TCL 令牌对
     * @throws ServiceException 开关关闭、并发冲突或 TCL 调用失败
     */
    public TclTokenVO issueByUid(Long uid, String legacyTokenHash) {
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        if (!oauth2Properties.getMigrate().isEnabled()) {
            // 本侧迁移开关关闭：视为通道未上线，不触发任何 TCL 调用
            Logger.info("本侧 TCL 令牌迁移开关未开启，跳过兑换: uid={}", uid);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        // 缓存命中直接复用：uid 维度缓存生效时同一用户全程只兑换一次
        TclTokenVO cached = readIssuedCache(uid);
        if (cached != null) {
            Logger.info("TCL 令牌兑换命中缓存，直接复用: uid={}", uid);
            return cached;
        }
        return issueWithLock(uid, legacyTokenHash);
    }

    /**
     * 获取用户当前可用的 TCL 令牌对（缓存优先，未命中时按需兑换）
     *
     * <p>与 {@link #issueByUid} 的差别在于缓存命中时<b>不受迁移开关约束</b>：
     * 直连登录链路（{@link #saveIssuedToken}）下发的令牌同样落在该缓存，
     * 开关关闭时也应可复用；只有缓存确实没有令牌时才回落到按需兑换。</p>
     *
     * @param uid 当前登录用户 uid
     * @return TCL 令牌对
     * @throws ServiceException 未登录、兑换开关关闭或兑换失败
     */
    public TclTokenVO getOrIssueToken(Long uid) {
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        TclTokenVO cached = readIssuedCache(uid);
        if (cached != null && cached.getAccessToken() != null && !cached.getAccessToken().isBlank()) {
            return cached;
        }
        return issueByUid(uid, null);
    }

    /**
     * 刷新 TCL access_token（前端调 TCL 接口遇令牌失效时使用）
     *
     * <p>刷新凭据是服务端代持的 refresh_token，与 TCL access_token 是否有效无关，
     * 因此不会出现「access 过期即无法刷新」的断链。TCL 返回 90009（凭证已失效）
     * 时清除兑换缓存，前端删除本地 TCL_ACCESS_TOKEN 后重新触发兑换即可自愈</p>
     *
     * @param uid 当前登录用户 uid
     * @return 新的 TCL 令牌对（refresh_token 已沿用旧值）
     * @throws ServiceException 无本地兑换记录或刷新失败
     */
    public TclTokenVO refreshByUid(Long uid) {
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        TclTokenVO cached = readIssuedCache(uid);
        if (cached == null || cached.getRefreshToken() == null || cached.getRefreshToken().isBlank()) {
            // 无本地刷新凭据：前端应删除本地 TCL_ACCESS_TOKEN 后重新触发兑换
            Logger.warn("TCL 令牌刷新失败：本地无兑换记录或缺少 refresh_token: uid={}", uid);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }

        long start = System.currentTimeMillis();
        try {
            TclTokenVO refreshed = tclMigrateClient.refreshToken(cached.getRefreshToken());
            // refresh_token 轮转：TCL 开启 rotateRefreshToken 时每次刷新会换发新值（旧值立即失效），
            // 必须持久化替换，否则下次刷新用旧值会被判为凭证泄露；未开启轮转时响应不含该字段，沿用旧值

            if (refreshed.getRefreshToken() == null || refreshed.getRefreshToken().isBlank()) {
                refreshed.setRefreshToken(cached.getRefreshToken());
            }
            refreshed.setUid(uid);
            if (refreshed.getScope() == null || refreshed.getScope().isBlank()) {
                refreshed.setScope(cached.getScope());
            }
            // 必须同步更新缓存中的 access_token，否则缓存会一直返回过期令牌
            writeIssuedCache(uid, refreshed);
            Logger.info("TCL 令牌刷新成功: uid={}, 耗时={}ms", uid, System.currentTimeMillis() - start);
            return refreshed;
        } catch (TclApiException e) {
            if (e.getTclCode() != null && e.getTclCode() == TclMigrateClient.TCL_CODE_REFRESH_TOKEN_INVALID) {
                // 凭证已被撤销（如另一设备登出触发 TCL 侧撤销）：清缓存待重新兑换自愈
                clearIssuedCache(uid);
                Logger.warn("TCL 刷新令牌已失效，已清除兑换缓存待重新兑换: uid={}", uid);
            }
            throw e;
        }
    }

    /**
     * 用服务端代持的 refresh_token 签发绑定游戏账号的 PAT
     *
     * <p>供「本人」场景的转发接口调用：凭证仍是本服务按 uid 保存的 refresh_token，
     * 不消费、不轮换，因此不影响正常登录态与后续刷新。签发失败（如 90009 凭证失效）
     * 时清除兑换缓存，待下次按需兑换自愈，与 {@link #refreshByUid} 处置一致。</p>
     *
     * @param uid       当前登录用户 uid（已由拦截器校验为本人）
     * @param request   名称、有效期、权限及固定账号的 payload
     * @return 新签发的 PAT（不含 refresh_token）
     * @throws ServiceException 未登录、无本地兑换记录或 TCL 调用失败
     */
    public TclPatVO issuePatByUid(Long uid, PatIssueRequestDTO request) {
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        // 先校验入参再扣限流额度、读取缓存：非法参数不消耗调用配额。
        validatePatParams(request);
        // 签发每次都真实请求 TCL，沿用已有 uid 限流键与额度。
        RedisRateLimiter.tryAcquire(redisTemplate, RedisKeyUtil.tclIssueRate(uid),
                PAT_ISSUE_MAX_PER_WINDOW, PAT_ISSUE_WINDOW_SECONDS,
                ResultCode.TOKEN_ISSUE_TOO_FREQUENTLY);

        TclTokenVO cached = readIssuedCache(uid);
        if (cached == null || cached.getRefreshToken() == null || cached.getRefreshToken().isBlank()) {
            // 无本地刷新凭据：前端应重新触发兑换后再签发 PAT。
            Logger.warn("TCL PAT 签发失败：本地无兑换记录或缺少 refresh_token: uid={}", uid);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }

        long start = System.currentTimeMillis();
        try {
            TclPatVO issued = tclMigrateClient.issuePatFromRefresh(cached.getRefreshToken(), request);
            Logger.info("TCL PAT 签发成功: uid={}, expires_in={}, 耗时={}ms",
                    uid, request.getExpiresIn(), System.currentTimeMillis() - start);
            return issued;
        } catch (TclApiException e) {
            if (e.getTclCode() != null && e.getTclCode() == TclMigrateClient.TCL_CODE_REFRESH_TOKEN_INVALID) {
                // 凭证已被撤销（如另一设备登出触发 TCL 侧撤销）：清缓存待重新兑换自愈
                clearIssuedCache(uid);
                Logger.warn("TCL 刷新令牌已失效，已清除兑换缓存待重新兑换: uid={}", uid);
            }
            throw e;
        }
    }

    /** 发送注销验证码，只使用当前用户缓存的 refresh_token。 */
    public void sendAccountDeletionCodeByUid(Long uid) {
        String refreshToken = requireAccountRefreshToken(uid);
        try {
            tclMigrateClient.sendAccountDeletionCode(refreshToken);
        } catch (TclApiException e) {
            clearInvalidAccountRefreshToken(uid, e);
            throw e;
        }
    }

    /** 委托用户中心注销账户；成功后的本地资料与凭证清理由会话服务统一处理。 */
    public void deleteAccountByUid(Long uid, String code) {
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        if (code == null || code.isBlank()) {
            throw new ServiceException(ResultCode.VERIFICATION_CODE_NOT_ENTER);
        }
        if (!code.matches("[0-9]{6}")) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID, "注销验证码必须为6位数字");
        }
        String refreshToken = requireAccountRefreshToken(uid);
        try {
            tclMigrateClient.deleteAccount(refreshToken, code);
        } catch (TclApiException e) {
            clearInvalidAccountRefreshToken(uid, e);
            throw e;
        }
    }

    private String requireAccountRefreshToken(Long uid) {
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        TclTokenVO cached = readIssuedCache(uid);
        if (cached == null || cached.getRefreshToken() == null || cached.getRefreshToken().isBlank()) {
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR,
                    "缺少账户注销凭证，请重新登录后重试");
        }
        return cached.getRefreshToken();
    }

    private void clearInvalidAccountRefreshToken(Long uid, TclApiException e) {
        if (e.getTclCode() != null && e.getTclCode() == TclMigrateClient.TCL_CODE_REFRESH_TOKEN_INVALID) {
            clearIssuedCache(uid);
        }
    }

    /**
     * 校验 PAT 签发入参。本站签发用途为读取指定游戏账号，因此要求 game_uid，
     * 避免签出因缺少账号而无法使用的凭证。绑定关系和权限上限由 TCL 校验。
     *
     * @param request PAT 签发参数
     * @throws ServiceException 参数不合法
     */
    static void validatePatParams(PatIssueRequestDTO request) {
        if (request == null) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }
        Long expiresIn = request.getExpiresIn();
        String name = request.getName();
        String scope = request.getScope();
        if (expiresIn == null || expiresIn <= 0 || expiresIn > PAT_MAX_EXPIRES_SECONDS) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID,
                    "expires_in 必须在 1~" + PAT_MAX_EXPIRES_SECONDS + " 秒之间");
        }
        if (name == null || name.isBlank()) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK, "name 不能为空");
        }
        if (name.trim().length() > PAT_NAME_MAX_LENGTH) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID,
                    "name 长度不能超过 " + PAT_NAME_MAX_LENGTH + " 字符");
        }
        if (scope != null && scope.length() > PAT_SCOPE_MAX_LENGTH) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID,
                    "scope 长度不能超过 " + PAT_SCOPE_MAX_LENGTH + " 字符");
        }
        if (request.getPayload() == null || request.getPayload().getGameUid() == null
                || request.getPayload().getGameUid().isBlank()) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK, "请选择 PAT 读取的游戏账号");
        }
        String payload = JsonMapper.toJSONString(request.getPayload());
        if (payload == null || payload.getBytes(StandardCharsets.UTF_8).length > 4096) {
            throw new ServiceException(ResultCode.PARAM_IS_INVALID, "payload 长度不能超过 4KB");
        }
    }

    /**
     * 登出双撤（R14）：撤销 TCL 侧授权并删除本地兑换缓存
     *
     * <p>本地 loginToken 的删除由调用方负责，两处必须同时执行：
     * 只删本地会造成 TCL 侧授权残留，只撤销 TCL 会造成用户仍处于登录态</p>
     *
     * @param uid 当前登录用户 uid
     */
    public void revokeByUid(Long uid) {
        if (uid == null) {
            return;
        }
        TclTokenVO cached = readIssuedCache(uid);
        if (cached != null) {
            // 优先撤销 refresh_token，使该会话的 TCL 授权整体失效
            String token = cached.getRefreshToken() != null ? cached.getRefreshToken() : cached.getAccessToken();
            tclMigrateClient.revokeToken(token);
        }
        clearIssuedCache(uid);
    }

    /**
     * 直连登录时调用（B8）：把 TCL 随 /oauth2/direct-user 一并签发的令牌落到服务端缓存
     *
     * <p>新登录链路与存量迁移链路共用同一份 uid 维度缓存，因此登录用户后续调用兑换接口
     * 会直接命中缓存，不再触发一次兑换；refresh_token 只在此处落服务端</p>
     *
     * @param uid            用户 uid
     * @param tclAccessToken  TCL access_token
     * @param tclRefreshToken TCL refresh_token
     * @param expiresIn      access_token 有效期（秒）
     * @param scope          授权范围
     */
    public void saveIssuedToken(Long uid, String tclAccessToken, String tclRefreshToken,
                               Long expiresIn, String scope) {
        if (uid == null || tclAccessToken == null || tclRefreshToken == null) {
            // TCL 未回带令牌（例如尚未开启令牌下发）时不写入，避免缓存出半截数据
            return;
        }
        TclTokenVO token = new TclTokenVO();
        token.setUid(uid);
        token.setAccessToken(tclAccessToken);
        token.setTokenType("Bearer");
        token.setExpiresIn(expiresIn);
        token.setRefreshToken(tclRefreshToken);
        token.setScope(scope);
        writeIssuedCache(uid, token);
    }

    /**
     * 带并发保护的首次兑换（进程内锁 + Redis 短锁 + 二次检查）
     *
     * @param uid             用户 uid
     * @param legacyTokenHash 旧自签 token 的 sha256（仅审计）
     * @return TCL 令牌对
     * @throws ServiceException 并发冲突或 TCL 调用失败
     */
    private TclTokenVO issueWithLock(Long uid, String legacyTokenHash) {
        ReentrantLock localLock = localLocks.computeIfAbsent(uid, key -> new ReentrantLock());
        localLock.lock();
        try {
            // 双重检查：等锁期间可能已有其他线程完成兑换
            TclTokenVO cached = readIssuedCache(uid);
            if (cached != null) {
                return cached;
            }

            String lockKey = RedisKeyUtil.tclLock(uid);
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(locked)) {
                // 其他实例正在兑换：本次不重复调用 TCL（避免击穿 TCL 侧限流），由前端稍后重试
                Logger.info("TCL 令牌兑换进行中，本次跳过: uid={}", uid);
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
            }

            long start = System.currentTimeMillis();
            try {
                TclTokenVO issued = tclMigrateClient.migrateToken(uid, legacyTokenHash);
                if (issued.getUid() == null) {
                    issued.setUid(uid);
                }
                writeIssuedCache(uid, issued);
                continuousFailCount.set(0);
                Logger.info("TCL 令牌兑换成功: uid={}, 耗时={}ms", uid, System.currentTimeMillis() - start);
                return issued;
            } catch (Exception e) {
                int failCount = continuousFailCount.incrementAndGet();
                Logger.warn("TCL 令牌兑换失败（不影响业务请求与登录态，前端可稍后重试）: uid={}, 连续失败={}次, 原因={}",
                        uid, failCount, e.getMessage());
                if (failCount >= FAIL_ALERT_THRESHOLD) {
                    Logger.error("TCL 令牌兑换连续失败 {} 次，请检查迁移端点可达性、IP 白名单与 Ed25519 签名配置", failCount);
                }
                throw e instanceof ServiceException ? (ServiceException) e
                        : new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
            } finally {
                redisTemplate.delete(lockKey);
            }
        } finally {
            localLock.unlock();
            localLocks.remove(uid, localLock);
        }
    }

    /**
     * 读取兑换缓存
     *
     * @param uid 用户 uid
     * @return TCL 令牌对；未缓存或解析失败返回 null
     */
    private TclTokenVO readIssuedCache(Long uid) {
        String json = redisTemplate.opsForValue().get(RedisKeyUtil.tclToken(uid));
        if (json == null || json.isBlank()) {
            return null;
        }
        return JsonMapper.parseObject(json, new TypeReference<TclTokenVO>() {
        });
    }

    /**
     * 写入兑换缓存（TTL 与本地会话一致）
     *
     * @param uid   用户 uid
     * @param token TCL 令牌对（含 refresh_token，只存服务端）
     */
    private void writeIssuedCache(Long uid, TclTokenVO token) {
        String json = JsonMapper.toJSONString(token);
        if (json == null) {
            return;
        }
        redisTemplate.opsForValue().set(RedisKeyUtil.tclToken(uid),
                json, ISSUED_CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 删除兑换缓存（登出双撤、刷新令牌失效自愈时调用）
     *
     * @param uid 用户 uid
     */
    private void clearIssuedCache(Long uid) {
        redisTemplate.delete(RedisKeyUtil.tclToken(uid));
    }
}
