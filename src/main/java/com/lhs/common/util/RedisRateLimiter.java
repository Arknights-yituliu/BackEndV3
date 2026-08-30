package com.lhs.common.util;

import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 限流工具：基于 INCR + 过期时间的固定窗口计数器
 * <p>
 * 统一系统中各处的限流逻辑（问卷提交频控、IP 频控、每日上传上限等），
 * 以静态方法调用，避免各调用点重复手写 INCR/EXPIRE/判断。
 * 参数使用 {@link RedisTemplate} 以兼容 StringRedisTemplate 与
 * {@code RedisTemplate<String, String>} / {@code RedisTemplate<String, Object>} 各类客户端。
 * </p>
 * <p>使用方式：</p>
 * <pre>
 *     // 超限抛指定业务异常
 *     RedisRateLimiter.tryAcquire(redisTemplate, "Key:" + id, 5, 60, ResultCode.EXCESSIVE_IP_ACCESS_TIMES);
 *
 *     // 超限静默拒绝（返回 false）
 *     if (!RedisRateLimiter.tryAcquire(redisTemplate, "Key:" + id, 5, 60)) {
 *         return;
 *     }
 * </pre>
 *
 * @author UserCenter
 */
public final class RedisRateLimiter {

    private RedisRateLimiter() {
    }

    /**
     * 尝试获取一次额度（固定窗口计数）
     * <p>首次调用（count==1）时写入窗口过期时间；窗口内计数超过 limit 返回 false 表示拒绝。
     * Redis 的 INCR 为原子操作，并发安全；窗口过期后计数自动清零重新计时。</p>
     *
     * @param redis      Redis 客户端（StringRedisTemplate / RedisTemplate&lt;String,String&gt; / RedisTemplate&lt;String,Object&gt;）
     * @param key        限流 key
     * @param limit      窗口内允许的最大次数
     * @param ttlSeconds 窗口时长（秒）
     * @return true=允许，false=超限
     */
    public static boolean tryAcquire(RedisTemplate<String, ?> redis, String key, long limit, long ttlSeconds) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return count == null || count <= limit;
    }

    /**
     * 尝试获取一次额度，超限时抛出指定业务异常
     *
     * @param redis      Redis 客户端
     * @param key        限流 key
     * @param limit      窗口内允许的最大次数
     * @param ttlSeconds 窗口时长（秒）
     * @param resultCode 超限时抛出的错误码
     */
    public static void tryAcquire(RedisTemplate<String, ?> redis, String key, long limit, long ttlSeconds, ResultCode resultCode) {
        if (!tryAcquire(redis, key, limit, ttlSeconds)) {
            throw new ServiceException(resultCode);
        }
    }
}
