package com.lhs.entity.dto.drop;

import lombok.Data;

/**
 * 回收站每日物品产出聚合结果，对应 RecycleDropMapper.selectDailyItemCount 的查询结果。
 */
@Data
public class RecycleDropDailyCountDTO {

    /** 物品 id */
    private String itemId;

    /** 当天该物品的产出总数量，一条 recycle_drop 明细视为一件产出 */
    private Long quantity;

    /** 当天该物品出现的上报批次数，同一批次内重复产出只算一次 */
    private Long times;
}
