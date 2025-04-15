package com.lhs.common.enums;

public enum UnitTime {

    ONE_HOUR(60 * 60 * 1000), // 一小时的毫秒数
    ONE_DAY(24 * 60 * 60 * 1000); // 一天的毫秒数

    private final long milliseconds;

    UnitTime(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    // 获取对应的毫秒数
    public long milliseconds() {
        return milliseconds;
    }
}
