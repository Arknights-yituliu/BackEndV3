package com.lhs.entity.dto.material;

import lombok.Data;


@Data
public class StageCalculationParametersDTO {
    //关卡掉落期望产出综合
    private Double countStageDropApValue;
    //关卡系列
    private String itemSeries;
    //关卡系列id
    private String itemSeriesId;
    //副产物id
    private String secondaryItemId;
    //关卡效率
    private Double stageEfficiency;

    {
        countStageDropApValue = 0.0;
        itemSeries = "empty";
        itemSeriesId = "empty";
        secondaryItemId = "empty";
        stageEfficiency = 0.0;
    }
}
