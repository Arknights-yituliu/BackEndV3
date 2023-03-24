package com.lhs.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RedisCacheable {
    String key() default "";     //缓存key
    boolean isArg() default false ;
    String argList() default "" ;    //参数列表中那些参数作为key拼接，填入的是一个或多个参数列表索引，示例 "0,1,2"
}