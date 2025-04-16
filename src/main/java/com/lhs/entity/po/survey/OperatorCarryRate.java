package com.lhs.entity.po.survey;


import java.util.Date;

public class OperatorCarryRate {
    private Long id;
    private String charId;
    private Integer carryCount;
    private Integer sampleSize;
    private Date createTime;

    private Integer questionnaireCode;
    private Date startTime;
    private Date endTime;

    private Integer recordType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCharId() {
        return charId;
    }

    public void setCharId(String charId) {
        this.charId = charId;
    }

    public Integer getCarryCount() {
        return carryCount;
    }

    public void setCarryCount(Integer carryCount) {
        this.carryCount = carryCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }



    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getQuestionnaireCode() {
        return questionnaireCode;
    }

    public void setQuestionnaireCode(Integer questionnaireCode) {
        this.questionnaireCode = questionnaireCode;
    }


    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getRecordType() {
        return recordType;
    }

    public void setRecordType(Integer recordType) {
        this.recordType = recordType;
    }

    @Override
    public String toString() {
        return "OperatorCarryRate{" +
                ", charId='" + charId + '\'' +
                ", carryCount=" + carryCount +
                ", sampleSize=" + sampleSize +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
