package com.lhs.entity.vo.item;

import com.lhs.entity.po.item.StageResult;
import com.lhs.entity.po.item.StageResultDetail;
import lombok.Data;


@Data
public class StageResultVOV2 {
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
    private Double leT5Efficiency;
    private Double leT4Efficiency;
    private Double leT3Efficiency;
    private Double stageEfficiency;
    private Integer sampleSize;
    private Double sampleConfidence;
    private String zoneName;
    private Integer stageColor;

    //从通用掉落信息中复制
    public void copyByStageResultCommon(StageResult stageResult){
        if(stageResult ==null){
            return;
        }
        this.stageId = stageResult.getStageId();
        this.stageCode = stageResult.getStageCode();
        this.stageType = stageResult.getStageType();
        this.secondaryItemId = stageResult.getSecondaryItemId();
        this.stageEfficiency = stageResult.getStageEfficiency();
        this.leT5Efficiency = stageResult.getLeT5Efficiency();
        this.leT4Efficiency = stageResult.getLeT4Efficiency();
        this.leT3Efficiency = stageResult.getLeT3Efficiency();

        this.spm = stageResult.getSpm();
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
    }






}
