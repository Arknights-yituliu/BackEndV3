package com.lhs.entity.dto.material;

import lombok.Data;

import java.util.Map;


@Data
public class StageParamDTO {
    private String version;  //API版本
    private Double expCoefficient; //经验书的系数,经验书价值=龙门币(0.0036)*系数
    private Integer sampleSize;  //样本量
    private Double lmdCoefficient; //龙门币系数   龙门币价值 =  根据钱本计算的龙门币价值(0.0036) * 龙门币系数
    private Boolean calculateActivityStage = false; //是否计算活动关卡
    private Map<String,String> itemBlacklist; //材料黑名单，计算中不使用这些材料
    private Map<String,String> stageBlacklist;  //关卡黑名单，计算中不使用这些关卡

    {
        version = "2024-11-09";
        expCoefficient = 0.625;
        sampleSize = 300;
        lmdCoefficient = 1.0;
    }

    //  返回版本号
    public String getVersion(){
        return version+"-"+ expCoefficient+"-"+sampleSize;
    }
}
