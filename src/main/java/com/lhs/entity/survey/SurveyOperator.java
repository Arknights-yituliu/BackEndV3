package com.lhs.entity.survey;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@TableName("survey_character_1")
@NoArgsConstructor
public class SurveyOperator {

    @TableId
    private Long id;

    private Long uid;

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
