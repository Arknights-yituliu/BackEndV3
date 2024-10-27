package com.lhs.entity.vo.material;

import com.lhs.entity.po.material.StageResultDetail;
import lombok.Data;

@Data
public class DropDetailVO {
    private String itemName;
    private String itemId;
    private Double knockRating;
    private Double apExpect;
    private Double result;
    private Double ratio;
    private Integer ratioRank;
    private Integer sampleSize;
    private Double sampleConfidence;

    public void copyByStageResultDetail(StageResultDetail stageResultDetail){
        this.itemName = stageResultDetail.getItemName();
        this.itemId = stageResultDetail.getItemId();
        this.knockRating = stageResultDetail.getKnockRating();
        this.apExpect = stageResultDetail.getApExpect();
        this.result = stageResultDetail.getResult();
        this.ratio = stageResultDetail.getRatio();
        this.ratioRank = stageResultDetail.getRatioRank();
        this.sampleSize = stageResultDetail.getSampleSize();
        this.sampleConfidence = stageResultDetail.getSampleConfidence();
    }
}
