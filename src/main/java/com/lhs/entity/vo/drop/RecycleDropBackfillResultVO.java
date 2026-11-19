package com.lhs.entity.vo.drop;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 回收站每日物品产出统计回填结果。
 */
@Data
public class RecycleDropBackfillResultVO {

    /** 计划回填的天数 */
    private Integer totalDays;

    /** 成功回填的天数 */
    private Integer successDays;

    /** 失败的天数 */
    private Integer failedDays;

    /** 成功回填的物品统计行数合计 */
    private Long totalItems;

    /** 失败日期（yyyy-MM-dd），全部成功时为空列表 */
    private List<String> failedDates = new ArrayList<>();
}
