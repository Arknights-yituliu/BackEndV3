package com.lhs.entity.vo.item;

import com.lhs.entity.po.item.StageResult;
import lombok.Data;

import java.util.List;

@Data
public class StageResultDetailVO {
    private String stageId;
    private String stageCode;
    private String stageType;
    private Double spm;
    private String mainItemId;
    private String itemSeries;
    private String itemSeriesId;
    private String secondaryItemId;
    private Double leT5Efficiency;
    private Double leT4Efficiency;
    private Double leT3Efficiency;
    private Double stageEfficiency;
    private Long endTime;
    private Integer zoneName;
    private List<DropDetailVO> dropDetailList;

    public void copyByStageResultCommon(StageResult stageResult){
        this.stageId = stageResult.getStageId();
        this.stageCode = stageResult.getStageCode();
        this.spm = stageResult.getSpm();
        this.itemSeriesId = stageResult.getItemSeriesId();
        this.secondaryItemId = stageResult.getSecondaryItemId();
        this.leT5Efficiency = stageResult.getLeT5Efficiency();
        this.leT4Efficiency = stageResult.getLeT4Efficiency();
        this.leT3Efficiency = stageResult.getLeT3Efficiency();
        this.stageEfficiency = stageResult.getStageEfficiency();
        this.itemSeries = stageResult.getItemSeries();
        this.endTime = stageResult.getEndTime().getTime();
    }

}
