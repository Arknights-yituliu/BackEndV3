package com.lhs.service.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "搓玉关卡效率结果返回对象")
public class OrundumPerApResultVo {

    @ApiModelProperty("关卡名称")
    private String stageCode;

    @ApiModelProperty("1理智可生成的合成玉")
    private Double orundumPerAp;

    @ApiModelProperty("关卡效率")
    private Double stageEfficiency;

    @ApiModelProperty("龙门币消耗")
    private Double lMDCost;

    @ApiModelProperty("搓玉效率（对比1-7的百分比）")
    private Double orundumPerApEfficiency;

}
