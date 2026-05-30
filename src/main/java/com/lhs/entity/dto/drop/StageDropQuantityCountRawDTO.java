package com.lhs.entity.dto.drop;

public class StageDropQuantityCountRawDTO {
    private String stageId;
    private String itemId;
    private Long quantity;
    private Long times;

    public StageDropQuantityCountRawDTO() {
    }

    public StageDropQuantityCountRawDTO(String stageId, String itemId, Long quantity, Long times) {
        this.stageId = stageId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.times = times;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
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

    public Long getTimes() {
        return times;
    }

    public void setTimes(Long times) {
        this.times = times;
    }
}
