package com.lhs.entity.vo.item;


import com.lhs.common.util.StageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class OrundumPerApResultVO {

    private String stageCode;

    private Double orundumPerAp;

    private Double stageEfficiency;

    private Double lMDCost;

    private Double orundumPerApEfficiency;

    private String stageType;

}
