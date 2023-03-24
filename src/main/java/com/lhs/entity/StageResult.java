package com.lhs.entity;

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
    private String stageId;  // 关卡id
    private String stageCode;   // 关卡名称
    private Double sampleConfidence; //置信度
    private Double spm;  //每分钟消耗理智
    private String zoneName;  //章节名称

    private String zoneId;  //区域Id
    private Integer isShow;   // 是否显示
    private Integer isValue;  //是否参与定价
    private Double apCost;  //理智消耗
    private String main; // 主材料
    private String secondary; // 副材料

    private String secondaryId; // 副材料id
    private String itemId;   //材料ID
    private String itemName;    //材料名称
    private String itemType;  //材料类型
    private Integer itemRarity;  //材料类型
    private Integer sampleSize;  // 样本次数
    private Double knockRating;   // 概率
    private Double apExpect; // 期望理智
    private Double result;   // 单项结果
    private Double efficiency; //绿票转化率
    private Double stageEfficiency;    //理智转化率
    private Integer stageColor ;   //关卡在前端显示的颜色
    private Double expCoefficient;   //关卡在前端显示的颜色
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date openTime;  //活动开启时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;


}
