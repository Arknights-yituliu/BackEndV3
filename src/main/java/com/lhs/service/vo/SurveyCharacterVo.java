package com.lhs.service.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyCharacterVo {
    private String charId;
    private Boolean own;
    private Integer level;
    private Integer elite;
    private Integer potential;
    private Integer rarity;
    private Integer skill1;
    private Integer skill2;
    private Integer skill3;
    private Integer modX;
    private Integer modY;

}
