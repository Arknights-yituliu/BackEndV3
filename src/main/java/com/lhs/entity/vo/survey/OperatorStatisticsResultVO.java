package com.lhs.entity.vo.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperatorStatisticsResultVO {

    private String charId;
    private Integer rarity;
    private Double own;
    private Map<String,Double> elite;
    private Map<String,Double> skill1;
    private Map<String,Double> skill2;
    private Map<String,Double> skill3;
    private Map<String,Double> modX;
    private Map<String,Double> modY;


}
