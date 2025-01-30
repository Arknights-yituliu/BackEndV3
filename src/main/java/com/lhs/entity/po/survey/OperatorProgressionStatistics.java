package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
public class OperatorProgressionStatistics {

    @TableId
    private Long id;
    private String  charId;
    private Integer own;
    private Integer rarity;
    private String potential;
    private String elite;
    private String skill1;
    private String skill2;
    private String skill3;
    private String modX;
    private String modY;
    private String modD;
    private String modA;
    private Integer recordType;
    private Date createTime;

}
