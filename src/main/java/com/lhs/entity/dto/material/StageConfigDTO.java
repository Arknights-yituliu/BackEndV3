package com.lhs.entity.dto.material;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;


@Data
public class StageConfigDTO {

    private Long id;
    //API版本
    private String version;
    //经验书的系数,经验书价值=龙门币(0.0036)*系数
    private Double expCoefficient;
    //样本量
    private Integer sampleSize;
    //龙门币系数   龙门币价值 =  根据钱本计算的龙门币价值(0.0036) * 龙门币系数
    private Double lmdCoefficient;
    //是否计算活动关卡
    private Boolean calculateActivityStage;
    //芯片是否按均价计算
    private Boolean chipIsValueConsistent;
    //材料黑名单，计算中不使用这些材料
    private Map<String, String> itemBlacklist;
    //关卡黑名单，计算中不使用这些关卡
    private Map<String, String> stageBlacklist;
    //强制指定某个材料的价值（例如无限池扭转醇）
    private Map<String, Double> customItemValue;

    {
        id = 202412050002L;
        version = "v4";
        expCoefficient = 0.633;
        chipIsValueConsistent = true;
        calculateActivityStage = false;
        sampleSize = 300;
        lmdCoefficient = 1.0;
        stageBlacklist =  new HashMap<>();
        customItemValue = new HashMap<>();
    }

    public Double getLMDValue() {
        return 0.0036 * lmdCoefficient;
    }

    public Double getEXPValue() {
        return 0.0036 * expCoefficient;
    }

    //  返回版本号
    public String getVersionCode() {
        return version + "-" + id;
    }
}
