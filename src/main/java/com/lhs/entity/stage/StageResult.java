package com.lhs.entity.stage;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("stage_result")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "关卡详细计算结果返回对象")
public class StageResult implements Serializable {

    @TableId
    @ApiModelProperty("索引id")
    private Long id;

    @ApiModelProperty("关卡id")
    private String stageId;  

    @ApiModelProperty("关卡名称")
    private String stageCode;

    @ApiModelProperty("章节名称")
    private String zoneName;

    @ApiModelProperty("区域Id")
    private String zoneId;

    @ApiModelProperty("主材料")
    private String main;

    @ApiModelProperty("材料名称")
    private String itemName;

    @ApiModelProperty("材料ID")
    private String itemId;

    @ApiModelProperty("副材料")
    private String secondary;

    @ApiModelProperty("副材料id")
    private String secondaryId;

    @ApiModelProperty("关卡是否参与定价")
    private Integer stageType;

    @ApiModelProperty("理智消耗")
    private Integer apCost;

    @ApiModelProperty("置信度")
    private Double sampleConfidence; 

    @ApiModelProperty("每分钟消耗理智")
    private Double spm;

    @ApiModelProperty("材料星级")
    private Integer itemRarity;

    @ApiModelProperty("材料类型")
    private String itemType;  

    @ApiModelProperty("样本次数")
    private Integer sampleSize;  

    @ApiModelProperty("概率")
    private Double knockRating;

    @ApiModelProperty("期望理智")
    private Double apExpect;

    @ApiModelProperty("单项结果")
    private Double result;

    @ApiModelProperty("产出占比")
    private Double ratio;

    @ApiModelProperty("占比排名")
    private Integer ratioRank;

    @ApiModelProperty("转化率")
    private Double stageEfficiency;

    @ApiModelProperty("版本")
    private String version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("活动开启时间")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("活动开启时间")
    private Date endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private Date updateTime;

    @ApiModelProperty("关卡等级")
    private Integer stageColor;

}
