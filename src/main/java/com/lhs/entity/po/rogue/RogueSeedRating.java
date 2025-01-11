package com.lhs.entity.po.rogue;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName
public class RogueSeedRating {
    @TableId
    private Long ratingId;
    private Long seedId;
    private Long uid;
    private Double rating;
    private Date createTime;
    private Date updateTime;
}
