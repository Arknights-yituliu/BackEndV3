package com.lhs.common.annotation;

import com.lhs.common.util.Result;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class AnnotationImpl {
    private  static final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    //扫描所有添加了@TakeCount注解的方法
    @Before("@annotation(takeCount)")
    public void takeCountBefore(TakeCount takeCount){
        startTime.set(System.currentTimeMillis());
    }

    //扫描所有添加了@RedisCacheable注解的方法
    @Around("@annotation(redisCacheable)")
    public Object redis(ProceedingJoinPoint pjp,RedisCacheable redisCacheable) throws Throwable {
        Object[] args = pjp.getArgs();
        String[] argNames = ((CodeSignature) pjp.getSignature()).getParameterNames();
        Map<String, Object> argMap = new HashMap<>();
        for (int i = 0; i < argNames.length; i++) {
            argMap.put(argNames[i], args[i]);
        }

        String[] key = redisCacheable.key().split("#");
        Object resultVo = "";
        if(key.length==1)  {
             resultVo = redisTemplate.opsForValue().get(key[0]);
        }else {
            StringBuilder redisKey = new StringBuilder(key[0]);
            for (int i = 1; i < key.length; i++) {
                redisKey.append(argMap.get(key[i]));
            }

            resultVo = redisTemplate.opsForValue().get(String.valueOf(redisKey));

        }
        log.info(pjp.getSignature().getName()+"缓存的内容：");
        if (resultVo != null) return Result.success(resultVo);
        log.info("数据库的内容：");
        return  pjp.proceed();
    }

    //接口方法执行完成之后
    @After("@annotation(takeCount)")
    public void takeCountAfter(TakeCount takeCount){
        log.info(takeCount.method()+"接口耗时："+(System.currentTimeMillis()-startTime.get())+"ms");
        startTime.remove();
    }




}
