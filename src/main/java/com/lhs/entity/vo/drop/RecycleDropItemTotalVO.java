package com.lhs.entity.vo.drop;

import lombok.Data;

/**
 * 回收站物品产出统计聚合结果（时间区间内按物品聚合）。
 */
@Data
public class RecycleDropItemTotalVO {

    /** 物品 id */
    private String itemId;

    /** 区间内该物品的产出总数量 */
    private Long quantity;

    /** 区间内所有物品的明细行数总和（不分材料），所有物品取到的值相同，作为掉率分母 */
    private Long times;
}
