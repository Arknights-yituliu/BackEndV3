package com.lhs.entity.dto.item.custom;


import lombok.Data;

//用于转换企鹅物流的api的实体类

public class PenguinMatrixDTO {

//    企鹅物流数据
    private String stageId;  //关卡id
    private String itemId; //物品id
    private Integer quantity;  //物品掉落次数
    private Integer times;  //关卡刷取次数
    private Long start;
    private Long end;


    public PenguinMatrixDTO() {
    }

    public PenguinMatrixDTO(String stageId, String itemId, Integer quantity, Integer times, Long start, Long end) {
        this.stageId = stageId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.times = times;
        this.start = start;
        this.end = end;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "PenguinMatrixDTO{" +
                "stageId='" + stageId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", quantity=" + quantity +
                ", times=" + times +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
