package com.lhs.entity.po.rougeSeed;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class RougeSeed{
    @TableId
    private Long seedId;
    private Long seed;
    private Integer rating;
    private String rougeVersion;
    private String rougeTheme;
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
