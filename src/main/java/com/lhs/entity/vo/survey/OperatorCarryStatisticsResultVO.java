package com.lhs.entity.vo.survey;


import java.util.ArrayList;
import java.util.List;

public class OperatorCarryStatisticsResultVO {
    private List<OperatorCarryVO> list;
    private String questionnaireType;
    private Integer questionnaireCode;
    private Long updateTime;
    private Integer sampleSize;



    public String getQuestionnaireType() {
        return questionnaireType;
    }

    public void setQuestionnaireType(String questionnaireType) {
        this.questionnaireType = questionnaireType;
    }

    public Integer getQuestionnaireCode() {
        return questionnaireCode;
    }

    public void setQuestionnaireCode(Integer questionnaireCode) {
        this.questionnaireCode = questionnaireCode;
    }

    public List<OperatorCarryVO> getList() {
        return list;
    }

    public void setList(List<OperatorCarryVO> list) {
        this.list = list;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }
}
