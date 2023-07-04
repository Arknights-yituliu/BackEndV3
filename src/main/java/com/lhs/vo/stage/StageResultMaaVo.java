package com.lhs.vo.stage;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "关卡效率结果返回对象")
@Data
public class StageResultMaaVo {
    @ApiModelProperty("关卡名称")
    private String stageCode;

    @ApiModelProperty("关卡id")
    private String stageId;

    @ApiModelProperty("物品id")
    private String itemId;

    @ApiModelProperty("物品类型")
    private String itemType;

    @ApiModelProperty("期望理智")
    private Double apExpect;

    @ApiModelProperty("副产物")
    private String secondary;

    @ApiModelProperty("物品掉落概率")
    private Double knockRating;

    @ApiModelProperty("理智转化率")
    private Double stageEfficiency;

    @ApiModelProperty("样本次数")
    private Integer sampleSize;

    @ApiModelProperty("活动名称")
    private String zoneName;

}
