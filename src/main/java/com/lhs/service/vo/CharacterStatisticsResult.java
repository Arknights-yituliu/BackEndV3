package com.lhs.service.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CharacterStatisticsResult {

    private String charId;
    private Integer rarity;
    private Double own;
    private HashMap<String,Double> elite;
    private HashMap<String,Double> skill1;
    private HashMap<String,Double> skill2;
    private HashMap<String,Double> skill3;
    private HashMap<String,Double> modX;
    private HashMap<String,Double> modY;


}
