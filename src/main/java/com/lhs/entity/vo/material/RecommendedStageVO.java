package com.lhs.entity.vo.material;

import java.util.List;

/**
 * 材料系列推荐关卡 VO
 * 一个材料系列（如固源岩组系）下的所有推荐关卡
 */
public class RecommendedStageVO {

    /** 材料系列ID（以蓝材料ID表示） */
    private String itemSeriesId;
    /** 该系列的关卡效率结果集合，按关卡效率降序排列 */
    private List<StageEfficiencyVO> stageResultList;

    public String getItemSeriesId() {
        return itemSeriesId;
    }

    public void setItemSeriesId(String itemSeriesId) {
        this.itemSeriesId = itemSeriesId;
    }

    public List<StageEfficiencyVO> getStageResultList() {
        return stageResultList;
    }

    public void setStageResultList(List<StageEfficiencyVO> stageResultList) {
        this.stageResultList = stageResultList;
    }
}
