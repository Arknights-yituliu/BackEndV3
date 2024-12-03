package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("survey_operator_statistics")
public class OperatorStatistics {

    @TableId
    private String  charId;
    private Integer own;
    private Integer rarity;
    private String potential;
    private String elite;
    private String skill1;
    private String skill2;
    private String skill3;
    private String modX;
    private String modY;
    private String modD;
    private String modA;

}
