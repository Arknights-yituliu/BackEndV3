package com.lhs.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级 Redis 缓存注解
 * <p>
 * 标注在方法上后由 {@link AnnotationAOP#redisCacheable} 环绕拦截：调用时先按 key 读 Redis，
 * 命中则直接返回缓存值、不再执行方法体；未命中则执行方法，并把返回值写入 Redis。
 * <p>
 * 缓存 key = key + 第一个参数生成的后缀（方法无参数或 paramOrMethod 为空时不追加后缀）。
 * <p>
 * 使用限制：
 * <ul>
 *   <li>基于 Spring AOP 代理实现，仅对 Spring 容器中 bean 的 public 方法生效；
 *       private 方法与同类内部自调用不经过代理，注解会被忽略</li>
 *   <li>返回值需可被 RedisTemplate 序列化，否则读缓存时反序列化会失败</li>
 *   <li>需要按参数区分缓存时必须通过 paramOrMethod 把参数拼进 key，否则所有调用共用同一份缓存</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RedisCacheable {

    /** 缓存 key 前缀，默认 test */
    String key() default "test";

    /** 缓存过期时间，单位秒，默认 3600；小于 0 表示永不过期 */
    int timeout() default 3600;

    /**
     * 缓存 key 后缀的生成方式，仅在方法存在参数时生效
     * <p>①留空：不追加后缀，直接使用 key 本身
     * <p>②填 param：将第一个参数转为字符串后以 - 拼接，作为唯一标识
     * <p>③填第一个参数对象的方法名：调用该方法，将其返回值以 - 拼接，作为唯一标识
     */
    String paramOrMethod() default "";
}