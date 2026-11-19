package com.lhs.entity.dto.drop;

import lombok.Data;

import java.util.Date;

/**
 * 回收站统计数据的日期范围，对应统计表中未过期数据的最小 / 最大统计日。
 */
@Data
public class RecycleDropStatDayRangeDTO {

    /** 最早统计日 */
    private Date startDay;

    /** 最晚统计日 */
    private Date endDay;
}
