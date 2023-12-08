package com.lhs.entity.dto.item;

import lombok.Data;

import java.util.List;


@Data
public class StageParamDTO {
    private String version = "v2";
    private Double expCoefficient = 0.625; //经验书的系数,经验书价值=龙门币(0.0036)*系数
    private Integer sampleSize = 300;  //样本量

    private Double lmdCoefficient = 1.0;

    private Boolean calculateActivityStage = false;

    private List<String> itemBlacklist;

    private List<String> stageBlacklist;



    //  返回版本号
    public String getVersion(){
        return version+"-"+ expCoefficient+"-"+sampleSize;
    }
}
