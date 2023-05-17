package com.lhs.entity.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyStatisticsChar {

    private String  charId;
    private Integer own;
    private Integer phase2;
    private Integer rarity;
    private String skill1;
    private String skill2;
    private String skill3;
    private String modX;
    private String modY;
    private String potential;
}
