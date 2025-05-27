package com.lhs.common.util;

import java.time.ZonedDateTime;
import java.util.Date;

public class TimeUtil {

    /**
     * 获取当前小时的整点，例如当前时间为16时多一点，获取16:00:00
     */
    public static Date getCurrentHourTime() {
        ZonedDateTime startOfHour = ZonedDateTime.now()
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return Date.from(startOfHour.toInstant());
    }

    /**
     * 获取当天的整点
     * 例如当前为2024-04-15 09:13:20  返回2024-04-15 00:00:00
     */
    public static Date getCurrentDayTime() {
        ZonedDateTime startOfHour = ZonedDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return Date.from(startOfHour.toInstant());
    }
}
