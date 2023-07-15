package com.lhs.entity.survey;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyCharacter {
    @ExcelProperty("id")
    private Long id;
    @ExcelProperty("uid")
    private Long uid;
    @ExcelProperty("角色id")
    private String charId;
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
}
