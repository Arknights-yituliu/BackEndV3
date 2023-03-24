package com.lhs.service.resultVo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class StageResultActVo {
    private String stageCode;   // 关卡名称
    private String itemId;  //物品id
    private String itemType;  // 物品类型
    private Double apExpect; // 期望理智
    private Double knockRating;   // 概率
    private Double stageEfficiency;    //理智转化率
    private Integer stageColor;  // 关卡标注颜色
    private String zoneName; //活动名称
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date openTime;
}
