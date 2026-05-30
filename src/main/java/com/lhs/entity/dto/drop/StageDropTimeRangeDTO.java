package com.lhs.entity.dto.drop;

import java.util.Date;

/**
 * selectTimeRange 查询返回值：时段内数据的首尾时间及总行数
 */
public class StageDropTimeRangeDTO {
    private Date firstTime;
    private Date lastTime;
    private Long cnt;

    public StageDropTimeRangeDTO() {
    }

    public StageDropTimeRangeDTO(Date firstTime, Date lastTime, Long cnt) {
        this.firstTime = firstTime;
        this.lastTime = lastTime;
        this.cnt = cnt;
    }

    public Date getFirstTime() {
        return firstTime;
    }

    public void setFirstTime(Date firstTime) {
        this.firstTime = firstTime;
    }

    public Date getLastTime() {
        return lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

    public Long getCnt() {
        return cnt;
    }

    public void setCnt(Long cnt) {
        this.cnt = cnt;
    }
}
