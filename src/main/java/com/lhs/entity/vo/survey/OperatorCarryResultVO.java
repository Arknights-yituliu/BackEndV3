package com.lhs.entity.vo.survey;


import java.util.ArrayList;
import java.util.List;

public class OperatorCarryResultVO {
    private List<OperatorCarryRateStatisticsVO> list;
    private Long updateTime;
    private Integer sampleSize;

    {
      list=new ArrayList<>();
      updateTime = 111111111111L;
      sampleSize = 0;
    }

    public List<OperatorCarryRateStatisticsVO> getList() {
        return list;
    }

    public void setList(List<OperatorCarryRateStatisticsVO> list) {
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
