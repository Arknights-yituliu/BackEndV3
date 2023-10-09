package com.lhs.entity.vo.stage;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lhs.entity.po.stage.StageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StageResultVO {

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

    private Integer sampleSize;
    private Double spm;
    private String itemType;
    private Integer itemRarity;
    private Double knockRating;
    private Double apExpect;
    private Double stageEfficiency;
    private Double itemRarityLessThan5Ratio;
    private Double itemRarityLessThan4Ratio;
    private Double itemRarityLessThan3Ratio;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
        this.sampleConfidence = stageResult.getSampleConfidence();
        this.sampleSize = stageResult.getSampleSize();
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
