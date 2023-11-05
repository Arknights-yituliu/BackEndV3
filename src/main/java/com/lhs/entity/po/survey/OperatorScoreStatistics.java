package com.lhs.entity.po.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorScoreStatistics {

    private String  charId;

    private Integer rarity;

    private Integer daily;

    private Integer sampleSizeDaily;

    private Integer rogue;

    private Integer sampleSizeRogue;

    private Integer securityService;

    private Integer sampleSizeSecurityService;

    private Integer hard;

    private Integer sampleSizeHard;

    private Integer universal;

    private Integer sampleSizeUniversal;

    private Integer counter;

    private Integer sampleSizeCounter;

    private Integer building;

    private Integer sampleSizeBuilding;

    private Integer comprehensive;

    private Integer sampleSizeComprehensive;


}
