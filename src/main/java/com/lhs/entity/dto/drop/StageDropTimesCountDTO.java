package com.lhs.entity.dto.drop;

public class StageDropTimesCountDTO {
    private String stageId;
    private String itemId;
    private Long times;

    public StageDropTimesCountDTO() {
    }

    public StageDropTimesCountDTO(String stageId, String itemId, Long times) {
        this.stageId = stageId;
        this.itemId = itemId;
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

    public Long getTimes() {
        return times;
    }

    public void setTimes(Long times) {
        this.times = times;
    }

    public void addTimes(Long times) {
        this.times += times;

    }
}
