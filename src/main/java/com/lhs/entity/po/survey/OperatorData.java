package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class OperatorData {

    @TableId
    private Long id;

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


    public void copyBySurveyOperatorData(SurveyOperatorData surveyOperatorData){
        this.id = surveyOperatorData.getId();
        this.akUid = surveyOperatorData.getAkUid();
        this.charId = surveyOperatorData.getCharId();
        this.own = surveyOperatorData.getOwn();
        this.level = surveyOperatorData.getLevel();
        this.elite = surveyOperatorData.getElite();
        this.potential = surveyOperatorData.getPotential();
        this.rarity = surveyOperatorData.getRarity();
        this.mainSkill = surveyOperatorData.getMainSkill();
        this.skill1 = surveyOperatorData.getSkill1();
        this.skill2 = surveyOperatorData.getSkill2();
        this.skill3 = surveyOperatorData.getSkill3();
        this.modX = surveyOperatorData.getModX();
        this.modY = surveyOperatorData.getModY();
        this.modD = surveyOperatorData.getModD();
        this.modA = surveyOperatorData.getModA();
    }
}
