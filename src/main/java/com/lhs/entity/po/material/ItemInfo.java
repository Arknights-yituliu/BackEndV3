package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("item")
public class ItemInfo {
    @TableId
    private Long id;
    private String itemId;  //物品id
    private String itemName; //物品名称
    private Double itemValueAp; //物品价值 兼容字段
    private Double itemValue; //物品价值 单位：理智
    private Integer rarity; //物品稀有度
    private Double weight;   //加工站爆率
}
