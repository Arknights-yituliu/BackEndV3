package com.lhs.entity.vo.survey;


public class OperatorCarryRateVO {
    private String charId;
    private Integer carryCount = 0;
    private Integer sampleSize = 0;

    public String getCharId() {
        return charId;
    }

    public void setCharId(String charId) {
        this.charId = charId;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Integer getCarryCount() {
        return carryCount;
    }

    public void setCarryCount(Integer carryCount) {
        this.carryCount = carryCount;
    }

    public void incrementSampleSize(Integer sampleSize){
        this.sampleSize+=sampleSize;
    }

    public void incrementCarryCount(Integer carryCount){
        this.carryCount+=carryCount;
    }
}
