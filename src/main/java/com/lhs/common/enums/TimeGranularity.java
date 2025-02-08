package com.lhs.common.enums;

public enum TimeGranularity {

    COUNT(0, "总计"),
    YEAR(1, "年"),
    MONTH(2, "月"),
    WEEK(3, "周"),
    DAY(4, "天"),
    HOUR(5, "时"),
    MINUTE(6, "分"),
    SECOND(7, "秒");

    private final int code;
    private final String description;

    TimeGranularity(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int code() {
        return code;
    }

    public String description() {
        return description;
    }


}
