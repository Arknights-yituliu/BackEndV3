package com.lhs.entity.vo.stage;

import com.lhs.entity.po.stage.StageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StageResultVo {

    private String stageId;
    private String stageCode;
    private String zoneName;
    private String zoneId;
    private String main;
    private String itemName;
    private String itemId;
    private String secondary;
    private String secondaryId;
    private Integer stageType;
    private Integer apCost;
    private Double sampleConfidence;
    private Double spm;
    private String itemType;
    private Integer itemRarity;
    private Double knockRating;
    private Double apExpect;
    private Double stageEfficiency;
    private Double itemRarityLessThan5Ratio;
    private Double itemRarityLessThan4Ratio;
    private Double itemRarityLessThan3Ratio;
    private Date updateTime;
    private Integer stageColor;

    public void copy(StageResult stageResult){
        this.stageId = stageResult.getStageId();
        this.stageCode = stageResult.getStageCode();
        this.zoneName = stageResult.getZoneName();
        this.zoneId = stageResult.getZoneId();
        this.main = stageResult.getMain();
        this.itemName = stageResult.getItemName();
        this.itemId = stageResult.getItemId();
        this.secondary = stageResult.getSecondary();
        this.secondaryId = stageResult.getSecondaryId();
        this.stageType = stageResult.getStageType();
        this.apCost = stageResult.getApCost();
        this.sampleConfidence = stageResult.getSampleConfidence();
        this.spm = stageResult.getSpm();
        this.itemType = stageResult.getItemType();
        this.itemRarity = stageResult.getItemRarity();
        this.knockRating = stageResult.getKnockRating();
        this.apExpect = stageResult.getApExpect();
        this.stageEfficiency = stageResult.getStageEfficiency();
        this.updateTime = stageResult.getUpdateTime();
        this.stageColor = stageResult.getStageColor();
    }




}
