package com.lhs.common.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 线程安全的 {@link DateFormat} 包装
 * <p>
 * {@link SimpleDateFormat} 内部持有可变的 {@code Calendar}，多个线程共用同一实例并发
 * {@code format()} 会互相踩状态，表现为「A 的时间被格式化成了 B 的时间」，甚至抛
 * {@code ArrayIndexOutOfBoundsException}，且极难复现。
 * <p>
 * 而 Jackson 的 {@code ObjectMapper#setDateFormat} 只是保存引用，之后由共享的
 * {@code DateSerializer} 直接调用，因此直接塞一个 {@code SimpleDateFormat} 进去就形成了
 * 跨线程共享。本类把真正干活的 {@code SimpleDateFormat} 放进 {@link ThreadLocal}，
 * 对外仍是同一个不可变的 {@code DateFormat} 实例，格式与原来完全一致。
 * <p>
 * 用法：{@code objectMapper.setDateFormat(new ThreadSafeDateFormat("yyyy-MM-dd HH:mm:ss"))}
 */
public class ThreadSafeDateFormat extends DateFormat {

    private static final long serialVersionUID = 1L;

    /** 格式模板 */
    private final String pattern;

    /** 区域设置，创建时固定 */
    private final Locale locale;

    /** 时区，创建时固定（SimpleDateFormat 默认取 JVM 默认时区，此处保持一致） */
    private final TimeZone timeZone;

    /** 每个线程各自持有自己的 SimpleDateFormat */
    private final transient ThreadLocal<SimpleDateFormat> holder;

    public ThreadSafeDateFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public ThreadSafeDateFormat(String pattern, Locale locale) {
        this.pattern = pattern;
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.timeZone = TimeZone.getDefault();
        this.holder = ThreadLocal.withInitial(this::createFormat);
    }

    private SimpleDateFormat createFormat() {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        format.setTimeZone(timeZone);
        return format;
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        return holder.get().format(date, toAppendTo, fieldPosition);
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        return holder.get().parse(source, pos);
    }

    @Override
    public Object clone() {
        return new ThreadSafeDateFormat(pattern, locale);
    }

    @Override
    public TimeZone getTimeZone() {
        return timeZone;
    }
}
