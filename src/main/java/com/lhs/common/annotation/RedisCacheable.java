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
 * timeout：缓存时间,单位s
 * <p>
 * paramOrMethod：此属性需要方法上有一个参数对象（且仅会调用第一个参数），同时有两种调用方式<br>
 *  ①填入param，会将第一个参数转为字符串拼接到key后，作为唯一标识<br>
 *  ②填入参数对象内部的方法名，调用参数内部对应方法，将返回的内容拼接到key后，作为唯一标识
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RedisCacheable {

    String key() default "test";

    int timeout() default 3600;

    String paramOrMethod() default "";
}