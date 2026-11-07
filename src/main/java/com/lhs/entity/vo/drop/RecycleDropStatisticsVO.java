package com.lhs.entity.vo.drop;

import lombok.Data;

import java.util.List;

/**
 * 回收站物品产出统计结果，带数据覆盖的日期范围。
 */
@Data
public class RecycleDropStatisticsVO {

    /** 数据开始日期（yyyy-MM-dd），无数据时为 null */
    private String startDay;

    /** 数据结束日期（yyyy-MM-dd），无数据时为 null */
    private String endDay;

    /** 每个物品的产出统计 */
    private List<RecycleDropItemTotalVO> items;
}
