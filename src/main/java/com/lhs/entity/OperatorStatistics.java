package com.lhs.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@TableName("operator_statistics")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperatorStatistics {
    private String charName;
    private String charId;
    private Integer rarity;
    private Integer holdings;
    private Integer phases1;
    private Integer phases2;
    private String potentialRanks;
    private String skill1;
    private String skill2;
    private String skill3;
}

