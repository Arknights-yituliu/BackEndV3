package com.lhs.entity.vo.stage;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;


@Data
public class StageResultVoV2 {
    private String zoneName;
    private String stageId;
    private String stageCode;
    private Integer stageType;

    private String itemType;
    private String itemTypeId;
    private String itemName;
    private String itemId;
    private String secondaryItemId;

    private Double apExpect;
    private Double knockRating;

    @TableField(value = "le_t5_efficiency")
    private Double leT5Efficiency;
    @TableField(value = "le_t4_efficiency")
    private Double leT4Efficiency;
    @TableField(value = "le_t3_efficiency")
    private Double leT3Efficiency;
    private Double stageEfficiency;

    private Integer sampleSize;
    private Double sampleConfidence;

    private Double spm;
    private String version;
    private Integer stageColor;


}
