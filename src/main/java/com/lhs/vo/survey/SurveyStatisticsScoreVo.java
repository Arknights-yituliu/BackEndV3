package com.lhs.vo.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyStatisticsScoreVo {

    private String  charId;

    private Integer rarity;

    private Double daily;

    private Integer sampleSizeDaily;

    private Double rogue;

    private Integer sampleSizeRogue;

    private Double securityService;

    private Integer sampleSizeSecurityService;

    private Double hard;

    private Integer sampleSizeHard;

    private Double universal;

    private Integer sampleSizeUniversal;

    private Double counter;

    private Integer sampleSizeCounter;

    private Double building;

    private Integer sampleSizeBuilding;

    private Double comprehensive;

    private Integer sampleSizeComprehensive;


}
