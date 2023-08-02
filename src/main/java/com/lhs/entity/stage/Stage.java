package com.lhs.entity.stage;

import com.alibaba.excel.annotation.ExcelProperty;
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

import java.util.Date;

@Data
@TableName("stage")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "关卡信息返回对象")
public class Stage {

    @TableId
    @ExcelProperty("关卡Id")
    @ApiModelProperty("索引id")
    private String stageId;

    @ExcelProperty("关卡名称")
    @ApiModelProperty("关卡名称")
    private String stageCode;

    @ExcelProperty("区域Id")
    @ApiModelProperty("区域Id")
    private String zoneId;

    @ExcelProperty("章节名称")
    @ApiModelProperty("章节名称")
    private String zoneName;

    @ExcelProperty("消耗理智")
    @ApiModelProperty("理智消耗")
    private Integer apCost;

    @ApiModelProperty("关卡类型")
    private Integer stageType;

    @ExcelProperty("活动开启时间")
    @ApiModelProperty("活动开启时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @ExcelProperty("活动关闭时间")
    @ApiModelProperty("活动关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @ApiModelProperty("每分钟消耗理智")
    private Double spm;

    private Integer minClearTime;



}
