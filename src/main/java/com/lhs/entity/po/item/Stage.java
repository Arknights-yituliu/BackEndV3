package com.lhs.entity.po.item;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

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
public class Stage {

    @TableId
    @ExcelProperty("关卡Id")
    private String stageId;

    @ExcelProperty("关卡名称")
    private String stageCode;

    @ExcelProperty("区域Id")
    private String zoneId;

    @ExcelProperty("章节名称")
    private String zoneName;

    @ExcelProperty("消耗理智")
    private Integer apCost;

    private String stageType;

    @ExcelProperty("活动开启时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @ExcelProperty("活动关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private Double spm;

    private Integer minClearTime;



}
