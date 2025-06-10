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
    private Double weight; //加工站爆率

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getItemValueAp() {
        return itemValueAp;
    }

    public void setItemValueAp(Double itemValueAp) {
        this.itemValueAp = itemValueAp;
    }

    public Double getItemValue() {
        return itemValue;
    }

    public void setItemValue(Double itemValue) {
        this.itemValue = itemValue;
    }

    public Integer getRarity() {
        return rarity;
    }

    public void setRarity(Integer rarity) {
        this.rarity = rarity;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
}
