package com.lhs.entity.po.rogueSeed;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class RogueSeed {
    @TableId
    private Long seedId;
    private String seed;
    private Double rating;
    private Integer ratingPerson;
    private String rogueVersion;
    private Integer difficulty;
    private String rogueTheme;
    private String squad;
    private String operatorTeam;
    private String description;
    private Long uid;
    private String tags;
    private String summaryImageLink;
    private Long createTime;
    private Long updateTime;
    private Boolean deleteFlag;

}
