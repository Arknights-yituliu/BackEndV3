package com.lhs.entity;

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
    private Integer phase2;
    private Integer level;
    private Integer rarity;
    private Integer skill1;
    private Integer skill2;
    private Integer skill3;
    private Integer modX;
    private Integer modY;
    private String potential_ranks;
}
