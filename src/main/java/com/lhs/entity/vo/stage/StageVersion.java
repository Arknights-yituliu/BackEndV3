package com.lhs.entity.vo.stage;

import lombok.Data;

@Data
public class StageVersion {
    private String type;
    private Double expCoefficient;
    private Integer sampleSize;


    public String getVersion(){
        return type+"."+expCoefficient;
    }
}
