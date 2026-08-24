package com.lhs.common.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Logger {

    /**
     * 打印 info 日志，支持 {} 占位符
     */
    public static void info(String message, Object... args) {
        log.info(message, args);
    }

      /**
     * 打印 error 日志，支持 {} 占位符
     */
    public static void warn(String message, Object... args) {
        log.warn(message, args);
    }

    /**
     * 打印 error 日志，支持 {} 占位符
     */
    public static void error(String message, Object... args) {
        log.error(message, args);
    }

    /**
     * 打印 error 日志，附带异常堆栈
     */
    public static void error(String message, Throwable t) {
        log.error(message, t);
    }
}
