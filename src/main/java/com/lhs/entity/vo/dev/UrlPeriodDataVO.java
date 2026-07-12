package com.lhs.entity.vo.dev;

/**
 * URL时间段访问量明细VO
 * 用于表示单个时间段（小时/天）的访问次数
 */
public class UrlPeriodDataVO {
    private String period;
    private Long count;

    public UrlPeriodDataVO() {
    }

    public UrlPeriodDataVO(String period, Long count) {
        this.period = period;
        this.count = count;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
