package com.lhs.entity.vo.survey;


import java.util.List;

public class OperatorCarryRateStatisticsVO {
    private List<OperatorCarryVO> list;

    private Integer sampleSize;





    public List<OperatorCarryVO> getList() {
        return list;
    }

    public void setList(List<OperatorCarryVO> list) {
        this.list = list;
    }


    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }
}
