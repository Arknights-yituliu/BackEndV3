package com.lhs.entity;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class SurveyDataChar {
    private String id;
    private Long uid;
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
