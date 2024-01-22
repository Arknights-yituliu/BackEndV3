package com.lhs.entity.dto.maa;

import lombok.Data;

@Data
public class BuildingData {
    private String charId;
    private String name;
    private String buffName;
    private String roomType;
    private String description;
    private String buffColor;
    private String textColor;
    private Integer phase;
    private Integer level;
    private Long timestamp;
}
