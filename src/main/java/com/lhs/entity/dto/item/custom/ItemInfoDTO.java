package com.lhs.entity.dto.item.custom;

public class ItemInfoDTO {
    private String itemId;
    private String itemName;
    private Double itemValue;
    private Double itemValueAp;
    private Integer rarity;
    private Double weight;
    private Integer cardNum;
    private Integer groupId;
    private String type;

    public ItemInfoDTO() {
    }

    public ItemInfoDTO(String itemId, String itemName, Double itemValue, Double itemValueAp, Integer rarity, Double weight, Integer cardNum, Integer groupId, String type) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemValue = itemValue;
        this.itemValueAp = itemValueAp;
        this.rarity = rarity;
        this.weight = weight;
        this.cardNum = cardNum;
        this.groupId = groupId;
        this.type = type;
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

    public Double getItemValue() {
        return itemValue;
    }

    public void setItemValue(Double itemValue) {
        this.itemValue = itemValue;
    }

    public Double getItemValueAp() {
        return itemValueAp;
    }

    public void setItemValueAp(Double itemValueAp) {
        this.itemValueAp = itemValueAp;
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

    public Integer getCardNum() {
        return cardNum;
    }

    public void setCardNum(Integer cardNum) {
        this.cardNum = cardNum;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @Override
    public String toString() {
        return "ItemInfo{" +
                "itemId='" + itemId + '\'' +
                ", itemName='" + itemName + '\'' +
                ", itemValue=" + itemValue +
                ", itemValueAp=" + itemValueAp +
                ", rarity=" + rarity +
                ", weight=" + weight +
                ", cardNum=" + cardNum +
                ", groupId=" + groupId +
                ", type='" + type + '\'' +
                '}';
    }
}
