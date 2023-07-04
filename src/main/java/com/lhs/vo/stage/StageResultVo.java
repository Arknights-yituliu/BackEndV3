package com.lhs.vo.stage;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@ApiModel(value = "关卡效率结果返回对象")
@Data
public class StageResultVo {
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

    @ApiModelProperty("副产物id")
    private String secondaryId;

    @ApiModelProperty("副产物")
    private String secondary;

    @ApiModelProperty("物品掉落概率")
    private Double knockRating;

    @ApiModelProperty("理智转化率")
    private Double stageEfficiency;

    @ApiModelProperty("样本次数")
    private Integer sampleSize;

    @ApiModelProperty("关卡标注颜色")
    private Integer stageColor;

    @ApiModelProperty("每分钟消耗理智")
    private Double spm;

    @ApiModelProperty("活动名称")
    private String zoneName;

    @ApiModelProperty("样本置信度")
    private Double sampleConfidence;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
