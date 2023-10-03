package com.lhs.common.annotation;

import com.lhs.common.entity.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Log;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class AnnotationAOP {
    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();


    private final RedisTemplate<String, Object> redisTemplate;

    public AnnotationAOP(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    //扫描所有添加了@TakeCount注解的方法
    @Before("@annotation(takeCount)")
    public void takeCountBefore(TakeCount takeCount) {
        startTime.set(System.currentTimeMillis());
    }

    //接口方法执行完成之后
    @After("@annotation(takeCount)")
    public void takeCountAfter(TakeCount takeCount) {
            long timeCost = (System.currentTimeMillis() - startTime.get());
            if (timeCost < 1000) {
                Log.info("执行方法："+takeCount.name() + "  耗时：" + timeCost + "ms");
            } else {
                Log.info("执行方法："+takeCount.name() + "  耗时：" + (timeCost / 1000) + "s");
            }

        startTime.remove();
    }

    @Before("@annotation(rateLimiter)")
    public void rateLimiter(RateLimiter rateLimiter){

        int time = rateLimiter.time();
        int maximumTimes = rateLimiter.MaximumTimes();
        String key = rateLimiter.key();

        Object cache = redisTemplate.opsForValue().get(key);

        if(cache!=null){
            int times = Integer.parseInt(String.valueOf(cache));

            if(times>maximumTimes) {
                throw new ServiceException(ResultCode.INTERFACE_TOO_MANY_EMAIL_SENT);
            }
            times++;
            redisTemplate.opsForValue().set(key,times,time,TimeUnit.SECONDS);
        }else {
            redisTemplate.opsForValue().set(key,1,time,TimeUnit.SECONDS);
        }

    }


    //扫描所有添加了@RedisCacheable注解的方法
    @Around("@annotation(redisCacheable)")
    public Object redisCacheable(ProceedingJoinPoint pjp, RedisCacheable redisCacheable) throws Throwable {
        Object[] args = pjp.getArgs();
        String[] argNames = ((CodeSignature) pjp.getSignature()).getParameterNames();
        Map<String, Object> argMap = new HashMap<>();
        for (int i = 0; i < argNames.length; i++) {
            argMap.put(argNames[i], args[i]);
        }

        int timeOut = redisCacheable.timeout();

        String[] key = redisCacheable.key().split("#");

        StringBuilder redisKey = new StringBuilder(key[0]);
        Object redisValue = "";
        if (key.length > 1) {
            for (int i = 1; i < key.length; i++) {
                redisKey.append(argMap.get(key[i]));
            }
        }

        redisValue = redisTemplate.opsForValue().get(String.valueOf(redisKey));
//        log.info(pjp.getSignature().getName() + "读取缓存内容");
        if (redisValue != null) return redisValue;
//        log.info("读取数据库内容");
        Object proceed = pjp.proceed();
        if(timeOut<0){
            redisTemplate.opsForValue().set(String.valueOf(redisKey), proceed);
        }else {
            redisTemplate.opsForValue().set(String.valueOf(redisKey), proceed, timeOut, TimeUnit.SECONDS);
        }

        log.info("存入redis");
        return proceed;
    }


}
