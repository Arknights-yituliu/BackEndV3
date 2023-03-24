package com.lhs.common.annotation;

import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.Result;
import com.lhs.common.util.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class AnnotationUtils {
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
        StringBuilder key = new StringBuilder(redisCacheable.key());
        if(redisCacheable.isArg()) {
            Arrays.stream(redisCacheable.argList().split(",")).forEach(index->key.append(args[Integer.parseInt(index)]));
        }
        Object resultVo = redisTemplate.opsForValue().get(key.toString());
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
