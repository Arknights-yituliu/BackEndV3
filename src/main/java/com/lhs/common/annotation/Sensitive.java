package com.lhs.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lhs.common.config.SensitiveSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerialize.class)
public @interface Sensitive {

/**
 * 前置不需要打码的长度
 */
        int prefixNoMaskLen() default 2;
/**
 * 后置不需要打码的长度
 */
        int suffixNoMaskLen() default 2;
/**
 * 用什么打码
 */
        String symbol() default "*";
}
