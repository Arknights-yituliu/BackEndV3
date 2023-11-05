package com.lhs.entity.vo.survey;

import com.alibaba.excel.annotation.ExcelProperty;
import com.lhs.entity.po.survey.OperatorData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperatorExportExcelVO {
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

    public void copy(OperatorData operatorData){
        this.charId = operatorData.getCharId();
        this.own = operatorData.getOwn();
        this.level = operatorData.getLevel();
        this.elite = operatorData.getElite();
        this.potential = operatorData.getPotential();
        this.rarity = operatorData.getRarity();
        this.skill1 = operatorData.getSkill1();
        this.skill2 = operatorData.getSkill2();
        this.skill3 = operatorData.getSkill3();
        this.modX = operatorData.getModX();
        this.modY = operatorData.getModY();
        this.modD = operatorData.getModD();
    }


}
