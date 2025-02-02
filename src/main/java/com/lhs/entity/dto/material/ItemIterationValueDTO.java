package com.lhs.entity.dto.material;

public class ItemIterationValueDTO {
    private String itemId;
    private String itemName;
    private Double iterationValue;


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

    public Double getIterationValue() {
        return iterationValue;
    }

    public void setIterationValue(Double iterationValue) {
        this.iterationValue = iterationValue;
    }
}
