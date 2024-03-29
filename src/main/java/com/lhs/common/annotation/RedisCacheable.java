package com.lhs.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RedisCacheable {
    String key() default "test";     //缓存key

    int timeout() default 3600;

    String params() default "";
}