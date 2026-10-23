package com.lhs.entity.dto.drop;

import lombok.Data;

/**
 * 回收站产出的单条明细：物品 id 与产出时间。
 */
@Data
public class RecycleDropItemDTO {

    /** 物品 id */
    private String itemId;

    /** 产出时间，毫秒级时间戳 */
    private Long dropTime;
}
