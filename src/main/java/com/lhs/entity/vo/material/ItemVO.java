package com.lhs.entity.vo.material;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemVO {

    private String itemId;  //物品id

    private String itemName; //物品名称

    private Double itemValue; //物品价值 单位：绿票

    private Double itemValueAp; //物品价值 单位：理智

    private Integer rarity; //物品稀有度

}
