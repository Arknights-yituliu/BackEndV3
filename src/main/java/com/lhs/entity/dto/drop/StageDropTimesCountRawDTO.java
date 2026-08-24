package com.lhs.entity.dto.drop;

public class StageDropTimesCountRawDTO {
    private String stageId;
    private Long times;

    public StageDropTimesCountRawDTO() {
    }

    public StageDropTimesCountRawDTO(String stageId, Long times) {
        this.stageId = stageId;
        this.times = times;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public Long getTimes() {
        return times;
    }

    public void setTimes(Long times) {
        this.times = times;
    }
}
