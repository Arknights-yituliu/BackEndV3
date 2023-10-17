package com.lhs.entity.dto.item;

import lombok.Data;


@Data
public class StageParamDTO {
    private String display = "public"; //是否公开
    private String version = "v2";

    private Double expCoefficient = 0.625; //经验书的系数,经验书价值=龙门币(0.0036)*系数
    private Integer sampleSize = 300;  //样本量

//  返回版本号
    public String getVersion(){
        return version+"-"+display+"-"+ expCoefficient+"-"+sampleSize;
    }
}
