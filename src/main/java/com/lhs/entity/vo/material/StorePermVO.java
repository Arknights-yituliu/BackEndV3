package com.lhs.entity.vo.material;

import lombok.Data;

@Data
public class StorePermVO {
    private String itemId;
    private String itemName;
    private String storeType;
    private Double cost;
    private Integer quantity;
    private Integer rarity;
    private Double costPer;
}
