package com.lhs.entity.vo.survey;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lhs.entity.po.survey.SurveyOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyOperatorExcelVO {
    @ExcelProperty("角色id")
    private String charId;
    @ExcelProperty("角色名称")
    private String name;
    @ExcelProperty("是否持有")
    private Boolean own;
    @ExcelProperty("等级")
    private Integer level;
    @ExcelProperty("精英等级")
    private Integer elite;
    @ExcelProperty("潜能")
    private Integer potential;
    @ExcelProperty("星级")
    private Integer rarity;
    @ExcelProperty("一技能")
    private Integer skill1;
    @ExcelProperty("二技能")
    private Integer skill2;
    @ExcelProperty("三技能")
    private Integer skill3;
    @ExcelProperty("X模组")
    private Integer modX;
    @ExcelProperty("Y模组")
    private Integer modY;
    @ExcelProperty("D模组")
    private Integer modD;

    public void copy(SurveyOperator surveyOperator){
        this.charId = surveyOperator.getCharId();
        this.own = surveyOperator.getOwn();
        this.level = surveyOperator.getLevel();
        this.elite = surveyOperator.getElite();
        this.potential = surveyOperator.getPotential();
        this.rarity = surveyOperator.getRarity();
        this.skill1 = surveyOperator.getSkill1();
        this.skill2 = surveyOperator.getSkill2();
        this.skill3 = surveyOperator.getSkill3();
        this.modX = surveyOperator.getModX();
        this.modY = surveyOperator.getModY();
        this.modD = surveyOperator.getModD();
    }


}
