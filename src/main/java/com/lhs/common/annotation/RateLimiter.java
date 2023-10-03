package com.lhs.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RateLimiter {

    String key() default "MaximumLimit"; //限流的key
    int time() default 10;  //限流的窗口时间
    int MaximumTimes() default 10;  //窗口时间内的最大次数

}
