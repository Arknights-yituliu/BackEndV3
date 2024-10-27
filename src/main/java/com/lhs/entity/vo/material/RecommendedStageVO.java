package com.lhs.entity.vo.material;

import lombok.Data;

import java.util.List;

@Data
public class RecommendedStageVO {
    private String itemType;
    private String itemTypeId;
    private String itemSeries;
    private String itemSeriesId;
    private String version;


    private List<StageResultVOV2> stageResultList;
}


