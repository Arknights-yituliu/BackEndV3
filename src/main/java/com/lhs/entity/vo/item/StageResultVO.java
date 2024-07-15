package com.lhs.entity.vo.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lhs.entity.po.material.StageResult;
import com.lhs.entity.po.material.StageResultDetail;
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
    private Double knockRating;
    private Double apExpect;
    private Double stageEfficiency;
    private Double leT4Efficiency;
    private Double leT3Efficiency;
    private Double leT2Efficiency;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private Integer stageColor;



    public void copyByStageResultCommon(StageResult stageResult){
        this.stageId = stageResult.getStageId();
        this.stageCode = stageResult.getStageCode();
        this.spm = stageResult.getSpm();
        this.itemType = stageResult.getItemSeries();
        this.stageEfficiency = stageResult.getStageEfficiency();
        this.leT4Efficiency = stageResult.getLeT4Efficiency();
        this.leT3Efficiency = stageResult.getLeT3Efficiency();
        this.leT2Efficiency = stageResult.getLeT2Efficiency();
        this.secondaryId = stageResult.getSecondaryItemId();
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
    }


}
