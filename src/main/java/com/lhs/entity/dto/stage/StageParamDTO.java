package com.lhs.entity.dto.stage;

import lombok.Data;


@Data
public class StageParamDTO {
    private String type = "public"; //是否公开
    private Double expCoefficient = 0.625; //经验书的系数,经验书价值=龙门币(0.0036)*系数
    private Integer sampleSize;  //样本量

//  返回版本号
    public String getVersion(){
        return type+"."+expCoefficient;
    }
}
