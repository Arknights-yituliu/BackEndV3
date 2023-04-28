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
    private Long holdings;
    private Long phases1;
    private Long phases2;
    private String potentialRanks;
    private String skill1;
    private String skill2;
    private String skill3;
}

