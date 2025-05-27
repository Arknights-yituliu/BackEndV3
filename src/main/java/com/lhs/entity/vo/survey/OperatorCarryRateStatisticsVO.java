package com.lhs.entity.vo.survey;


import java.util.List;

public class OperatorCarryRateStatisticsVO {
    private List<OperatorCarryRateVO> list;

    private Integer sampleSize;





    public List<OperatorCarryRateVO> getList() {
        return list;
    }

    public void setList(List<OperatorCarryRateVO> list) {
        this.list = list;
    }


    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }
}
