package com.lhs.entity.vo.material;

import lombok.Data;
import java.util.List;

@Data
public class ActStageVO {
    private String zoneName;
    private String stageType;
    private List<ActStageResultVO> actStageList;
    private Long endTime;

}
