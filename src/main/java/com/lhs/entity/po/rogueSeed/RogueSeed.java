package com.lhs.entity.po.rogueSeed;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class RogueSeed {
    @TableId
    private Long seedId;
    private String seed;
    private Integer ratingCount;
    private Integer rating;
    private String rogueVersion;
    private Integer difficulty;
    private String rogueTheme;
    private String squad;
    private String operatorTeam;
    private String description;
    private Long uid;
    private String tags;
    private String summaryImageLink;
    private Date createTime;
    private Date updateTime;
    private Boolean deleteFlag;

}
