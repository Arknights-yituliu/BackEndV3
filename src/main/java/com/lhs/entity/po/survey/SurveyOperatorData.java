package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@TableName("survey_operator_1")
public class SurveyOperatorData {

    @TableId
    private Long id;

    private Long uid;

    private String akUid;

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

    private Integer modD;

    private Integer modA;
}
