package com.lhs.entity.dto.drop;

import java.util.Date;

public class StageDropQuantityDTO {
    private String stageId;
    private String itemId;
    private Date start;
    private Date end;

    private Long quantity;

    public StageDropQuantityDTO() {
    }

    public StageDropQuantityDTO(String stageId, String itemId, Date start, Date end, Long quantity) {
        this.stageId = stageId;
        this.itemId = itemId;
        this.start = start;
        this.end = end;

        this.quantity = quantity;
    }

    public String getStageId() {
        return stageId;
    }

    public String getItemId() {
        return itemId;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(Long quantity) {
        this.quantity += quantity;
    }

}
