package com.lhs.entity.vo.item;

import com.lhs.entity.po.item.StageResultCommon;
import com.lhs.entity.po.item.StageResultDetail;
import lombok.Data;

@Data
public class StageResultDetailVO {

    private String stageId;
    private String stageCode;
    private String stageType;
    private Double spm;
    private String itemName;
    private String itemId;
    private String secondaryItemId;
    private Integer itemRarity;
    private Double apExpect;
    private Double knockRating;
    private Double ratio;
    private Double leT5Efficiency;
    private Double leT4Efficiency;
    private Double leT3Efficiency;
    private Double stageEfficiency;
    private Integer sampleSize;
    private Double sampleConfidence;

    //从通用掉落信息中复制
    public void copyByStageResultCommon(StageResultCommon stageResultCommon){
        if(stageResultCommon ==null){
            return;
        }
        this.stageId = stageResultCommon.getStageId();
        this.stageCode = stageResultCommon.getStageCode();
        this.stageType = stageResultCommon.getStageType();
        this.secondaryItemId = stageResultCommon.getSecondaryItemId();
        this.stageEfficiency = stageResultCommon.getStageEfficiency();
        this.leT5Efficiency = stageResultCommon.getLeT5Efficiency();
        this.leT4Efficiency = stageResultCommon.getLeT4Efficiency();
        this.leT3Efficiency = stageResultCommon.getLeT3Efficiency();

        this.spm = stageResultCommon.getSpm();
    }

    //从详细掉落信息中复制
    public void copyByStageResultDetail(StageResultDetail stageResultDetail){
        if(stageResultDetail ==null){
            return;
        }
        this.itemName = stageResultDetail.getItemName();
        this.itemId = stageResultDetail.getItemId();
        this.apExpect = stageResultDetail.getApExpect();
        this.knockRating = stageResultDetail.getKnockRating();
        this.sampleSize = stageResultDetail.getSampleSize();
        this.sampleConfidence = stageResultDetail.getSampleConfidence();
        this.itemRarity = stageResultDetail.getItemRarity();
        this.ratio = stageResultDetail.getRatio();
    }
}
