package com.lhs.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.exception.UcApiException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.entity.dto.user.UcTokenVO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * UC 令牌迁移服务：存量登录态按需兑换、刷新与撤销
 *
 * <p>自签 token 是 BackEndV3 的会话权威（鉴权只查本地 loginToken，不校验 UC 令牌），
 * 本服务只负责为用户额外取得一份 UC 授权，使前端可直连 UC 接口：</p>
 * <ul>
 *   <li>按需兑换（B4）：兑换结果按 <b>uid 维度</b>缓存到 {@code uc:migrate:issued:{uid}}，
 *       同一用户全程只兑换一次，既避免重复兑换击穿 UC 侧 5/分钟限流，
 *       也对齐 UC 侧「(uid, client_id) 上恒一条迁移凭证」的幂等模型——
 *       若按自签 token 分片，多设备会各兑换一次并互相撤销，旧设备刷新即掉登录；</li>
 *   <li>并发去重（B6）：进程内锁 + Redis 短锁，多标签页同时触发时只有一个真正调用 UC；</li>
 *   <li>失败降级（B7）：兑换失败只让兑换接口返回失败，不影响业务请求与登录态
 *       （鉴权仍走本地 loginToken），前端下次进入业务页/启动时重试；连续失败达阈值告警；</li>
 *   <li>刷新与撤销（B12 / R14）：服务端代持 refresh_token 刷新；登出时与本地 loginToken 双撤。</li>
 * </ul>
 *
 * @author BackEndV3
 */
@Service
public class UcTokenMigrateService {

    /**
     * 兑换缓存有效期（天）：与本地 loginToken 的 90 天一致，覆盖 UC refresh_token 生命周期
     */
    private static final long ISSUED_CACHE_TTL_DAYS = 90L;

    /** 并发去重短锁持有时长（秒）：只需覆盖一次兑换的往返时间 */
    private static final long LOCK_TTL_SECONDS = 10L;

    /** 连续兑换失败告警阈值：达到即打 error 日志，提示排查迁移端点与签名配置 */
    private static final int FAIL_ALERT_THRESHOLD = 10;

    private final OAuth2Properties oauth2Properties;
    private final UcMigrateClient ucMigrateClient;
    private final RedisTemplate<String, String> redisTemplate;

    /** 进程内按 uid 的兑换锁：与 Redis 短锁配合，避免单实例内多线程重复调用 UC */
    private final ConcurrentHashMap<Long, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    /** 连续兑换失败次数（成功即清零），用于触发告警日志 */
    private final AtomicInteger continuousFailCount = new AtomicInteger();

    public UcTokenMigrateService(OAuth2Properties oauth2Properties,
                                 UcMigrateClient ucMigrateClient,
                                 RedisTemplate<String, String> redisTemplate) {
        this.oauth2Properties = oauth2Properties;
        this.ucMigrateClient = ucMigrateClient;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 兑换 UC 令牌（前端主动调用的兑换接口使用）
     *
     * <p>缓存命中直接复用；未命中才签名调用 UC。失败时抛出异常让接口返回失败，
     * 前端不阻塞业务（自签 token 仍可正常调 BackEndV3），下次进入业务页再重试</p>
     *
     * @param uid             当前登录用户 uid
     * @param legacyTokenHash 旧自签 token 的 sha256（仅作 UC 侧审计）
     * @return UC 令牌对
     * @throws ServiceException 开关关闭、并发冲突或 UC 调用失败
     */
    public UcTokenVO issueByUid(Long uid, String legacyTokenHash) {
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        if (!oauth2Properties.getMigrate().isEnabled()) {
            // 本侧迁移开关关闭：视为通道未上线，不触发任何 UC 调用
            Logger.info("本侧 UC 令牌迁移开关未开启，跳过兑换: uid={}", uid);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
        // 缓存命中直接复用：uid 维度缓存生效时同一用户全程只兑换一次
        UcTokenVO cached = readIssuedCache(uid);
        if (cached != null) {
            Logger.info("UC 令牌兑换命中缓存，直接复用: uid={}", uid);
            return cached;
        }
        return issueWithLock(uid, legacyTokenHash);
    }

    /**
     * 刷新 UC access_token（前端调 UC 接口遇 401 时使用）
     *
     * <p>刷新凭据是服务端代持的 refresh_token，与 UC access_token 是否有效无关，
     * 因此不会出现「access 过期即无法刷新」的断链。UC 返回 90009（凭证已失效）
     * 时清除兑换缓存，前端删除本地 UC_ACCESS_TOKEN 后重新触发兑换即可自愈</p>
     *
     * @param uid 当前登录用户 uid
     * @return 新的 UC 令牌对（refresh_token 已沿用旧值）
     * @throws ServiceException 无本地兑换记录或刷新失败
     */
    public UcTokenVO refreshByUid(Long uid) {
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_LOGIN);
        }
        UcTokenVO cached = readIssuedCache(uid);
        if (cached == null || cached.getRefreshToken() == null || cached.getRefreshToken().isBlank()) {
            // 无本地刷新凭据：前端应删除本地 UC_ACCESS_TOKEN 后重新触发兑换
            Logger.warn("UC 令牌刷新失败：本地无兑换记录或缺少 refresh_token: uid={}", uid);
            throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }

        long start = System.currentTimeMillis();
        try {
            UcTokenVO refreshed = ucMigrateClient.refreshToken(cached.getRefreshToken());
            // refresh_token 不轮换：UC 未返回新值时沿用旧值，本地记录无需变更
            refreshed.setRefreshToken(cached.getRefreshToken());
            refreshed.setUid(uid);
            if (refreshed.getScope() == null || refreshed.getScope().isBlank()) {
                refreshed.setScope(cached.getScope());
            }
            // 必须同步更新缓存中的 access_token，否则缓存会一直返回过期令牌
            writeIssuedCache(uid, refreshed);
            Logger.info("UC 令牌刷新成功: uid={}, 耗时={}ms", uid, System.currentTimeMillis() - start);
            return refreshed;
        } catch (UcApiException e) {
            if (e.getUcCode() != null && e.getUcCode() == UcMigrateClient.UC_CODE_REFRESH_TOKEN_INVALID) {
                // 凭证已被撤销（如另一设备登出触发 UC 侧撤销）：清缓存待重新兑换自愈
                clearIssuedCache(uid);
                Logger.warn("UC 刷新令牌已失效，已清除兑换缓存待重新兑换: uid={}", uid);
            }
            throw e;
        }
    }

    /**
     * 登出双撤（R14）：撤销 UC 侧授权并删除本地兑换缓存
     *
     * <p>本地 loginToken 的删除由调用方负责，两处必须同时执行：
     * 只删本地会造成 UC 侧授权残留，只撤销 UC 会造成用户仍处于登录态</p>
     *
     * @param uid 当前登录用户 uid
     */
    public void revokeByUid(Long uid) {
        if (uid == null) {
            return;
        }
        UcTokenVO cached = readIssuedCache(uid);
        if (cached != null) {
            // 优先撤销 refresh_token，使该会话的 UC 授权整体失效
            String token = cached.getRefreshToken() != null ? cached.getRefreshToken() : cached.getAccessToken();
            ucMigrateClient.revokeToken(token);
        }
        clearIssuedCache(uid);
    }

    /**
     * 直连登录时调用（B8）：把 UC 随 /oauth2/direct-user 一并签发的令牌落到服务端缓存
     *
     * <p>新登录链路与存量迁移链路共用同一份 uid 维度缓存，因此登录用户后续调用兑换接口
     * 会直接命中缓存，不再触发一次兑换；refresh_token 只在此处落服务端</p>
     *
     * @param uid            用户 uid
     * @param ucAccessToken  UC access_token
     * @param ucRefreshToken UC refresh_token
     * @param expiresIn      access_token 有效期（秒）
     * @param scope          授权范围
     */
    public void saveIssuedToken(Long uid, String ucAccessToken, String ucRefreshToken,
                               Long expiresIn, String scope) {
        if (uid == null || ucAccessToken == null || ucRefreshToken == null) {
            // UC 未回带令牌（例如尚未开启令牌下发）时不写入，避免缓存出半截数据
            return;
        }
        UcTokenVO token = new UcTokenVO();
        token.setUid(uid);
        token.setAccessToken(ucAccessToken);
        token.setTokenType("Bearer");
        token.setExpiresIn(expiresIn);
        token.setRefreshToken(ucRefreshToken);
        token.setScope(scope);
        writeIssuedCache(uid, token);
    }

    /**
     * 带并发保护的首次兑换（进程内锁 + Redis 短锁 + 二次检查）
     *
     * @param uid             用户 uid
     * @param legacyTokenHash 旧自签 token 的 sha256（仅审计）
     * @return UC 令牌对
     * @throws ServiceException 并发冲突或 UC 调用失败
     */
    private UcTokenVO issueWithLock(Long uid, String legacyTokenHash) {
        ReentrantLock localLock = localLocks.computeIfAbsent(uid, key -> new ReentrantLock());
        localLock.lock();
        try {
            // 双重检查：等锁期间可能已有其他线程完成兑换
            UcTokenVO cached = readIssuedCache(uid);
            if (cached != null) {
                return cached;
            }

            String lockKey = RedisKeyUtil.ucMigrateLock(uid);
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(locked)) {
                // 其他实例正在兑换：本次不重复调用 UC（避免击穿 UC 侧限流），由前端稍后重试
                Logger.info("UC 令牌兑换进行中，本次跳过: uid={}", uid);
                throw new ServiceException(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
            }

            long start = System.currentTimeMillis();
            try {
                UcTokenVO issued = ucMigrateClient.migrateToken(uid, legacyTokenHash);
                if (issued.getUid() == null) {
                    issued.setUid(uid);
                }
                writeIssuedCache(uid, issued);
                continuousFailCount.set(0);
                Logger.info("UC 令牌兑换成功: uid={}, 耗时={}ms", uid, System.currentTimeMillis() - start);
                return issued;
            } catch (Exception e) {
                int failCount = continuousFailCount.incrementAndGet();
                Logger.warn("UC 令牌兑换失败（不影响业务请求与登录态，前端可稍后重试）: uid={}, 连续失败={}次, 原因={}",
                        uid, failCount, e.getMessage());
                if (failCount >= FAIL_ALERT_THRESHOLD) {
                    Logger.error("UC 令牌兑换连续失败 {} 次，请检查迁移端点可达性、IP 白名单与 Ed25519 签名配置", failCount);
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
     * @return UC 令牌对；未缓存或解析失败返回 null
     */
    private UcTokenVO readIssuedCache(Long uid) {
        String json = redisTemplate.opsForValue().get(RedisKeyUtil.ucMigrateIssued(uid));
        if (json == null || json.isBlank()) {
            return null;
        }
        return JsonMapper.parseObject(json, new TypeReference<UcTokenVO>() {
        });
    }

    /**
     * 写入兑换缓存（TTL 与本地会话一致）
     *
     * @param uid   用户 uid
     * @param token UC 令牌对（含 refresh_token，只存服务端）
     */
    private void writeIssuedCache(Long uid, UcTokenVO token) {
        String json = JsonMapper.toJSONString(token);
        if (json == null) {
            return;
        }
        redisTemplate.opsForValue().set(RedisKeyUtil.ucMigrateIssued(uid),
                json, ISSUED_CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 删除兑换缓存（登出双撤、刷新令牌失效自愈时调用）
     *
     * @param uid 用户 uid
     */
    private void clearIssuedCache(Long uid) {
        redisTemplate.delete(RedisKeyUtil.ucMigrateIssued(uid));
    }
}
