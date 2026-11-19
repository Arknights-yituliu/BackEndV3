package com.lhs.entity.dto.drop;

import lombok.Data;

import java.util.List;

/**
 * 回收站产出的单条明细：物品 id、产出时间与产出该物品的干员。
 */
@Data
public class RecycleDropItemDTO {

    /** 物品 id */
    private String itemId;

    /** 产出时间，毫秒级时间戳 */
    private Long dropTime;

    /** 产出该物品的干员 id 列表 */
    private List<String> operators;
}
