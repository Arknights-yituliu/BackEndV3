package com.lhs.entity.vo.item;

import com.lhs.entity.po.material.Stage;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ZoneTableVO {
    private String zoneName;
    private String zoneId;
    private String stageType;
    private Date endTime;
    private List<Stage> stageList;
}
