package com.lhs.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperatorData {
    private String id;
    private Long uploaderId;
    private String charId;
    private String charName;
    private Boolean own;
    private Integer elite;
    private Integer level;
    private Integer potential;
    private Integer rarity;
    private Long createTimeStamp;
}
