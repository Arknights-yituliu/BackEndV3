package com.lhs.entity.dto.material;

public class CustomItemDTO {
    private String itemId;
    private Double itemValue;

    public CustomItemDTO() {
    }

    public CustomItemDTO(String itemId, Double itemValue) {
        this.itemId = itemId;
        this.itemValue = itemValue;
    }

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
}
