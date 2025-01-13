package com.lhs.common.annotation;

import com.lhs.common.util.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            LogUtils.info("执行方法：" + takeCount.name() + "  耗时：" + timeCost + "ms");
        } else {
            LogUtils.info("执行方法：" + takeCount.name() + "  耗时：" + (timeCost / 1000) + "s");
        }

        startTime.remove();
    }

    @Around("@annotation(rateLimiter)")
    public Object rateLimiter(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {

        //获取方法的参数列表
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        //限流的窗口时间
        int time = rateLimiter.time();
        //窗口时间内的最大次数
        int maximumTimes = rateLimiter.MaximumTimes();
        //限流的key前缀，后面可能根据参数拼接
        String keyPrefix = rateLimiter.key();

        // 假设只有一个参数，且该参数是自定义实体类

        String keyField = rateLimiter.keyField();

        return joinPoint.proceed();

    }

    private void RedisLimiter(String key, Integer time, Integer maximumTimes) {
        Boolean hasKey = redisTemplate.hasKey(key);
        Long expireTime = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (hasKey == null || !hasKey || expireTime == null || expireTime <= 0) {
            redisTemplate.expire(key, time, TimeUnit.SECONDS);
        }
        // 增加访问计数，原子操作以确保并发安全

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return;
        }
        if (count < maximumTimes) {
            return;
        }

        throw new ServiceException(ResultCode.NOT_REPEAT_REQUESTS);


    }


    //扫描所有添加了@RedisCacheable注解的方法
    @Around("@annotation(redisCacheable)")
    public Object redisCacheable(ProceedingJoinPoint joinPoint, RedisCacheable redisCacheable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        String cacheKey = redisCacheable.key();

        if(args.length>0){
            cacheKey += generateCacheKey(args[0], redisCacheable.keyMethod());
        }


        Object cache = "";

        cache = redisTemplate.opsForValue().get(cacheKey);
//        log.info(pjp.getSignature().getName() + "读取缓存内容");
        if (cache != null) {
            return cache;
        }
//        log.info("读取数据库内容");
        Object proceed = joinPoint.proceed();

        int timeOut = redisCacheable.timeout();
        if (timeOut < 0) {
            redisTemplate.opsForValue().set(String.valueOf(cacheKey), proceed);
        } else {
            redisTemplate.opsForValue().set(String.valueOf(cacheKey), proceed, timeOut, TimeUnit.SECONDS);
        }

        log.info("数据已缓存，缓存key:" + cacheKey);
        return proceed;
    }

    /**
     * 生成缓存键。
     *
     * @param param     方法参数，可以是基本类型或自定义对象。根据此参数生成缓存键。
     * @param keyMethod 如果参数是自定义对象，则指定用于生成缓存键的方法名；如果是基本类型或不需要调用方法获取键，则为空字符串。
     * @return 返回一个字符串形式的缓存键，该键将用于Redis中存储和检索缓存数据。
     */
    private String generateCacheKey(Object param, String keyMethod) {
        // 检查参数是否为null，如果为null则返回空字符串作为缓存键
        if (param == null) {
            return "";
        }

        // 如果keyMethod为空字符串，说明参数是基本类型或者不需要通过方法获取key
        if (keyMethod.isEmpty()) {
            // 直接使用参数的toString()方法生成缓存键
            // 注意：对于基本类型和String类型，这通常是安全的；但对于其他类型的对象，需要确保其toString()方法能够唯一标识对象
            return param.toString();
        } else {
            try {
                // 如果提供了keyMethod，尝试在参数对象上调用该方法
                // 获取参数对象的Class对象，并查找名为keyMethod的方法
                Method method = param.getClass().getMethod(keyMethod);

                // 调用该方法并获取返回值作为缓存键
                Object keyValue = method.invoke(param);

                // 将返回值转换为字符串形式，如果没有返回值（即返回null），则返回空字符串
                return keyValue != null ? keyValue.toString() : "";
            } catch (NoSuchMethodException e) {
                // 如果没有找到指定的方法，抛出运行时异常
                return keyMethod + " does not exist";
//                throw new RuntimeException("The specified key method '" + keyMethod + "' does not exist in the parameter class.", e);
            } catch (IllegalAccessException | InvocationTargetException e) {
                // 如果方法访问失败或方法执行过程中抛出异常，抛出运行时异常
                return "Failed to invoke the key method " + keyMethod;
//                throw new RuntimeException("Failed to invoke the key method '" + keyMethod + "'.", e);
            }
        }
    }
}
