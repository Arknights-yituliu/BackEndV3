package com.lhs.entity.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyStatisticsCharacter {

    private String  charId;
    private Integer own;
    private Integer rarity;
    private String elite;
    private Integer sampleSizeElite;
    private String skill1;
    private Integer sampleSizeSkill1;
    private String skill2;
    private Integer sampleSizeSkill2;
    private String skill3;
    private Integer sampleSizeSkill3;
    private String modX;
    private Integer sampleSizeModX;
    private String modY;
    private Integer sampleSizeModY;
    private String potential;
    private Integer sampleSizePotential;
}
