package com.lhs.entity.dto.material;

public class ItemCustomValueDTO {

    private String itemId;

    private Double itemValue;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Double getItemValue() {
        return itemValue;
    }

    public void setItemValue(Double itemValue) {
        this.itemValue = itemValue;
    }

    @Override
    public String toString() {
        return "ItemCustomValue{" +
                "itemName='" + itemId + '\'' +
                ", itemValue='" + itemValue + '\'' +
                '}';
    }
}
