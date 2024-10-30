package com.lhs.entity.dto.rogueSeed;

import lombok.Data;

import java.util.List;


@Data
public class RogueSeedDTO {
    private Long seedId;
    private String seed;
    private Long uid;
    private String rogueVersion;
    private String rogueTheme;
    private String squad;
    private String operatorTeam;
    private String description;
    private List<String> tags;
    private String summaryImageLink;

}
