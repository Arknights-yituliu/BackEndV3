package com.lhs.common.annotation;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class AnnotationAOP {
    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    //扫描所有添加了@TakeCount注解的方法
    @Before("@annotation(takeCount)")
    public void takeCountBefore(TakeCount takeCount) {
        startTime.set(System.currentTimeMillis());
    }

    //接口方法执行完成之后
    @After("@annotation(takeCount)")
    public void takeCountAfter(TakeCount takeCount) {
        long timeCost =  (System.currentTimeMillis() - startTime.get());
        if(timeCost<1000) {
            log.info(takeCount.method() + "接口耗时：" + timeCost + "ms");
        }else {
            log.info(takeCount.method() + "接口耗时：" + (timeCost/1000) + "s");
        }



        startTime.remove();
    }


    //扫描所有添加了@RedisCacheable注解的方法
    @Around("@annotation(redisCacheable)")
    public Object redis(ProceedingJoinPoint pjp, RedisCacheable redisCacheable) throws Throwable {
        Object[] args = pjp.getArgs();
        String[] argNames = ((CodeSignature) pjp.getSignature()).getParameterNames();
        Map<String, Object> argMap = new HashMap<>();
        for (int i = 0; i < argNames.length; i++) {
            argMap.put(argNames[i], args[i]);
        }

        int timeOut = redisCacheable.timeOut();

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
        redisTemplate.opsForValue().set(String.valueOf(redisKey), proceed, timeOut, TimeUnit.SECONDS);
        log.info("存入redis");
        return proceed;
    }



}
