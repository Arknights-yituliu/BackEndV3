package com.lhs.entity.po.stage;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("stage_result_v2")
public class StageResultV2 {

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
    private String version;
}
