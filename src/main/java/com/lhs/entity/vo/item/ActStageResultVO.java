package com.lhs.entity.vo.item;

import lombok.Data;

@Data
public class ActStageResultVO {
    private String stageId;
    private String stageCode;
    private String itemId;
    private String itemName;
    private Double apExpect;
    private Double knockRating;
    private Double stageEfficiency;
}
