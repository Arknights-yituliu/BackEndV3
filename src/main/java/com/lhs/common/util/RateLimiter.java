package com.lhs.common.util;

import com.lhs.common.exception.ServiceException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimiter {

    private static final String KEY_PREFIX = "rate_limit_";


    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void tryAcquire(String id, int maxRequests, int timeWindowInSeconds,ResultCode resultCode) {
        String key = KEY_PREFIX + id;
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime + timeWindowInSeconds * 1000L;

        // 获取当前计数器值
        String value = redisTemplate.opsForValue().get(key);
        int count = value == null ? 0 : Integer.parseInt(value);

        if (count >= maxRequests) {
            // 如果超过最大请求数，则不允许新的请求
            throw new ServiceException(resultCode);

        }

        // 增加计数器并设置过期时间
        redisTemplate.opsForValue().set(key, String.valueOf(count + 1), timeWindowInSeconds, TimeUnit.SECONDS);

    }
}