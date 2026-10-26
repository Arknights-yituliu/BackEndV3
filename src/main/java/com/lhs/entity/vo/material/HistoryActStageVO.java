package com.lhs.entity.vo.material;

import java.util.List;

/**
 * 历史活动关卡 VO
 * 按活动（章节）聚合的活动关卡效率结果
 */
public class HistoryActStageVO {

    /** 所属章节（活动名称） */
    private String zoneName;
    /** 关卡类型 */
    private String stageType;
    /** 该活动下的关卡结果集合 */
    private List<StageEfficiencyVO> actStageList;
    /** 活动结束时间戳 */
    private Long endTime;

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getStageType() {
        return stageType;
    }

    public void setStageType(String stageType) {
        this.stageType = stageType;
    }

    public List<StageEfficiencyVO> getActStageList() {
        return actStageList;
    }

    public void setActStageList(List<StageEfficiencyVO> actStageList) {
        this.actStageList = actStageList;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
