package com.lhs.entity.vo.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lhs.entity.po.item.StageResult;
import com.lhs.entity.po.item.StageResultCommon;
import com.lhs.entity.po.item.StageResultDetail;
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
    private String stageType;
    private Integer apCost;
    private Double sampleConfidence;
    private Integer sampleSize;
    private Double spm;
    private String itemType;
    private Integer itemRarity;
    private Double knockRating;
    private Double apExpect;
    private Double stageEfficiency;
    private Double leT5Efficiency;
    private Double leT4Efficiency;
    private Double leT3Efficiency;
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

    public void copyByStageResultCommon(StageResultCommon stageResultCommon){
        this.stageId = stageResultCommon.getStageId();
        this.stageCode = stageResultCommon.getStageCode();
        this.stageType = stageResultCommon.getStageType();
        this.spm = stageResultCommon.getSpm();
        this.itemType = stageResultCommon.getItemType();

        this.stageEfficiency = stageResultCommon.getStageEfficiency();
        this.leT5Efficiency = stageResultCommon.getLeT5Efficiency();
        this.leT4Efficiency = stageResultCommon.getLeT4Efficiency();
        this.leT3Efficiency = stageResultCommon.getLeT3Efficiency();
        this.secondaryId = stageResultCommon.getSecondaryItemId();
    }

    public void copyByStageResultDetail(StageResultDetail stageResultDetail){
        this.stageId = stageResultDetail.getStageId();
        this.itemName = stageResultDetail.getItemName();
        this.itemId = stageResultDetail.getItemId();
        this.main = stageResultDetail.getItemName();
        this.sampleConfidence = stageResultDetail.getSampleConfidence();
        this.sampleSize = stageResultDetail.getSampleSize();
        this.knockRating = stageResultDetail.getKnockRating();
        this.apExpect = stageResultDetail.getApExpect();
        this.itemRarity = stageResultDetail.getItemRarity();
    }


}
