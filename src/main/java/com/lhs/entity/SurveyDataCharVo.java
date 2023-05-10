package com.lhs.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class SurveyDataCharVo {
    private String charId;
    private Integer phase;
    private Integer level;
    private Integer potential;
    private Integer rarity;
    private Integer skill1;
    private Integer skill2;
    private Integer skill3;
    private Integer modX;
    private Integer modY;
}
