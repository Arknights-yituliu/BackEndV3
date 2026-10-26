package com.lhs.entity.vo.material;

import java.util.List;

/**
 * 推荐关卡汇总结果 VO
 * 对应前端 stageEfficiencyCal.js 中 getStageData 的返回值
 */
public class StageEfficiencyResultVO {

    /** 各材料系列的开放中推荐关卡 */
    private List<RecommendedStageVO> recommendedStage;
    /** 搓玉推荐关卡 */
    private List<StageEfficiencyVO> orundumRecommendedStageVO;
    /** 历史活动关卡（按活动聚合） */
    private List<HistoryActStageVO> historyActStage;
    /** 历史活动搓玉关卡（按活动聚合） */
    private List<HistoryActStageVO> historyActOrundumStage;

    public List<RecommendedStageVO> getRecommendedStage() {
        return recommendedStage;
    }

    public void setRecommendedStage(List<RecommendedStageVO> recommendedStage) {
        this.recommendedStage = recommendedStage;
    }

    public List<StageEfficiencyVO> getOrundumRecommendedStageVO() {
        return orundumRecommendedStageVO;
    }

    public void setOrundumRecommendedStageVO(List<StageEfficiencyVO> orundumRecommendedStageVO) {
        this.orundumRecommendedStageVO = orundumRecommendedStageVO;
    }

    public List<HistoryActStageVO> getHistoryActStage() {
        return historyActStage;
    }

    public void setHistoryActStage(List<HistoryActStageVO> historyActStage) {
        this.historyActStage = historyActStage;
    }

    public List<HistoryActStageVO> getHistoryActOrundumStage() {
        return historyActOrundumStage;
    }

    public void setHistoryActOrundumStage(List<HistoryActStageVO> historyActOrundumStage) {
        this.historyActOrundumStage = historyActOrundumStage;
    }
}
