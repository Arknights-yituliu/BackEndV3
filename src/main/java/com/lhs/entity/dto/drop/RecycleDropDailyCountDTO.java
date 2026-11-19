package com.lhs.entity.dto.drop;

import lombok.Data;

/**
 * 回收站每日物品产出聚合结果，对应 RecycleDropMapper.selectDailyItemCount 的查询结果。
 */
@Data
public class RecycleDropDailyCountDTO {

    /** 物品 id */
    private String itemId;

    /** 该物品的产出总数量，一条 recycle_drop 明细视为一件产出 */
    private Long quantity;

    /** 统计区间内所有物品的明细行数总和（不分材料），作为掉率分母，每个物品取到的值相同 */
    private Long times;
}
