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

    /** 区间内该物品的上报批次数总和 */
    private Long times;
}
