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

    @ApiModelProperty("置信度")
    private Double sampleConfidence; 

    @ApiModelProperty("每分钟消耗理智")
    private Double spm;  

    @ApiModelProperty("章节名称")
    private String zoneName;  

    @ApiModelProperty("区域Id")
    private String zoneId;  

    @ApiModelProperty("关卡是否显示在前端")
    private Integer isShow;   

    @ApiModelProperty("关卡是否参与定价")
    private Integer isValue;  

    @ApiModelProperty("理智消耗")
    private Double apCost;  

    @ApiModelProperty("主材料")
    private String main; 

    @ApiModelProperty("副材料")
    private String secondary; 

    @ApiModelProperty("副材料id")
    private String secondaryId; 

    @ApiModelProperty("材料ID")
    private String itemId;   

    @ApiModelProperty("材料名称")
    private String itemName;    

    @ApiModelProperty("材料类型")
    private String itemType;  

    @ApiModelProperty("材料等级")
    private Integer itemRarity;  

    @ApiModelProperty("样本次数")
    private Integer sampleSize;  

    @ApiModelProperty("概率")
    private Double knockRating;   

    @ApiModelProperty("期望理智")
    private Double apExpect; 

    @ApiModelProperty("单项结果")
    private Double result;   

    @ApiModelProperty("转化率")
    private Double efficiency; 

    @ApiModelProperty("转化率百分比")
    private Double stageEfficiency;    

    @ApiModelProperty("关卡在前端显示的颜色")
    private Integer stageColor ;   

    @ApiModelProperty("经验书的价值系数")
    private Double expCoefficient;   

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("活动开启时间")
    private Date openTime;  

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    private Date updateTime;


}
