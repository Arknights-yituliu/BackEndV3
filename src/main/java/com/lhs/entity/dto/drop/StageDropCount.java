package com.lhs.entity.dto.drop;

public class StageDropCount {
    private String stageId;
    private String itemId;
    private Integer times;
    private Integer quantity;

    public StageDropCount() {
    }

    public StageDropCount(String stageId, String itemId, Integer times, Integer quantity) {
        this.stageId = stageId;
        this.itemId = itemId;
        this.times = times;
        this.quantity = quantity;
    }

    public String getStageId() {
        return stageId;
    }

    public String getItemId() {
        return itemId;
    }

    public Integer getTimes() {
        return times;
    }

    public Integer getQuantity() {
        return quantity;
    }
    

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(Integer quantity){
        this.quantity += quantity;
    }

    public void addTimes(Integer times){
        this.times += times;
    }

}
