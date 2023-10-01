package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("survey_character_1")
public class SurveyOperatorVo {
    private String charId;
    private Boolean own;
    private Integer level;
    private Integer elite;
    private Integer potential;
    private Integer rarity;
    private Integer mainSkill;
    private Integer skill1;
    private Integer skill2;
    private Integer skill3;
    private Integer modX;
    private Integer modY;

}
