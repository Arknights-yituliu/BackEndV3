package com.lhs.entity.vo.drop;

public class StageDropStatisticsResultVO {

    private String stageId;
    private String itemId;
    private Long quantity;
    private Long times;
    private Long start;
    private Long end;

    public StageDropStatisticsResultVO() {
    }

    public StageDropStatisticsResultVO(String stageId, String itemId, Long quantity, Long times, Long start, Long end) {
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
        return "StageDropStatisticsResultVO{" +
                "stageId='" + stageId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", quantity=" + quantity +
                ", times=" + times +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
