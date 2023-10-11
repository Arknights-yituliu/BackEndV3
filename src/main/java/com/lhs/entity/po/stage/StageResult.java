package com.lhs.entity.po.stage;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

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
public class StageResult implements Serializable {

    @TableId
    private Long id;

    private String stageId;  

    private String stageCode;

    private String zoneName;

    private String zoneId;

    private String main;

    private String itemName;

    private String itemId;

    private String secondary;

    private String secondaryId;

    private Integer stageType;

    private Integer apCost;

    private Double sampleConfidence; 

    private Double spm;

    private Integer itemRarity;

    private String itemType;

    private Integer sampleSize;

    private Double knockRating;

    private Double apExpect;

    private Double result;

    private Double ratio;

    private Integer ratioRank;

    private Double stageEfficiency;

    private String version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    private Integer stageColor;

}
