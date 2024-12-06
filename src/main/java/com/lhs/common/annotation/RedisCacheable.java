package com.lhs.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义缓存注解
 * <p>
 * <p>
 * key：缓存key
 * <p>
 * timeout：缓存时间
 * <p>
 * keyMethod：如果需要根据传入的参数在key中追求唯一标识（对象或基本类型都可以）但是仅可使用第一个参数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RedisCacheable {

    String key() default "test";

    int timeout() default 3600;

    String keyMethod() default "";
}