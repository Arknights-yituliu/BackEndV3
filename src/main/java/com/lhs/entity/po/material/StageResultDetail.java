package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.util.Date;

@Data
@TableName("stage_result_detail")
public class StageResultDetail {

    @TableId
    private Long id;
    private String stageId;
    private String itemName;
    private String itemId;
    private Double knockRating;
    private Double apExpect;
    private Double result;
    private Double ratio;
    private Integer ratioRank;
    private Integer sampleSize;
    private Double sampleConfidence;
    private Date endTime;
    private String version;

}
