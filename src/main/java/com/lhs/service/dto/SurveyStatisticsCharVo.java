package com.lhs.service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyStatisticsCharVo {
    private String  charId;
    private Integer own;
    private Integer phase2;
    private Integer rarity;
    private Map<Integer,Long> skill1;
    private Map<Integer,Long> skill2;
    private Map<Integer,Long> skill3;
    private Map<Integer,Long> modX;
    private Map<Integer,Long> modY;
    private Map<Integer,Long> potential;
}
