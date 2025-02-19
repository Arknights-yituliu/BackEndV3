package com.lhs.common.util;

import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimiter {

    private static final String KEY_PREFIX = "Limit:";


    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 限制特定时间窗口内可请求的最大次数
     * @param key  请求方法的key
     * @param maxRequests 最大请求数
     * @param timeWindowInSeconds 时间窗口（单位秒）
     * @param resultCode 报错信息
     */
    public void tryAcquire(String key, int maxRequests, int timeWindowInSeconds, ResultCode resultCode) {
        key = KEY_PREFIX + key;
        Long currentTime = System.currentTimeMillis();

        // 使用原子操作递增计数器，并获取递增后的值
        Long countObj = redisTemplate.opsForValue().increment(key);
        long count = (countObj == null) ? 0 : countObj; // 避免潜在的 NullPointerException

        // 如果是新创建的key（即count=1），则设置过期时间
        if (count == 1) {
            redisTemplate.expire(key, timeWindowInSeconds, TimeUnit.SECONDS);
        }

        // 检查是否超过最大请求数
        if (count > maxRequests) {
            // 如果超过最大请求数，则不允许新的请求
            throw new ServiceException(resultCode);
        }

    }
}