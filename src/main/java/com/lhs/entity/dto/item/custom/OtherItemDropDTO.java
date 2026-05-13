package com.lhs.entity.dto.item.custom;

public class OtherItemDropDTO {
    private String itemId;
    private Long quantity;
    private String itemName;
    private Double price;
    private String itemValue;

    public OtherItemDropDTO() {
    }
    public OtherItemDropDTO(String itemId, Long quantity, String itemName, Double price, String itemValue) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.itemName = itemName;
        this.price = price;
        this.itemValue = itemValue;
    }

    public String getItemId() {
        return itemId;
    }
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    public Long getQuantity() {
        return quantity;
    }
    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }
    public String getItemValue() {
        return itemValue;
    }
    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }

    @Override
    public String toString() {
        return "ShopUnlimitedItem{" +
                "itemId='" + itemId + '\'' +
                ", quantity=" + quantity +
                ", itemName='" + itemName + '\'' +
                ", price=" + price +
                ", itemValue='" + itemValue + '\'' +
                '}';
    }
}
