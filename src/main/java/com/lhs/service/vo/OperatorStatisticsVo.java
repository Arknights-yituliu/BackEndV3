package com.lhs.service.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperatorStatisticsVo {

    private String charName;
    private Integer rarity;
    private String charId;
    private Double owningRate;
    private Double phases1Rate;
    private Double phases2Rate;
    private JSONObject potentialRanks;

}
