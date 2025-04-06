package com.lhs.entity.vo.survey;

import com.lhs.entity.dto.survey.OperatorProgressionStatisticalResultDTO;

import java.util.List;

public class OperatorProgressionStatisticalResultVO {

    private Integer sampleSize;
    private List<OperatorProgressionStatisticalResultDTO> result;
    private Integer recordType;
    private Long createTime;

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public List<OperatorProgressionStatisticalResultDTO> getResult() {
        return result;
    }

    public void setResult(List<OperatorProgressionStatisticalResultDTO> result) {
        this.result = result;
    }

    public Integer getRecordType() {
        return recordType;
    }

    public void setRecordType(Integer recordType) {
        this.recordType = recordType;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
