package com.lhs.entity.po.rogueSeed;

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
    private Integer rating;
    private Date createTime;
    private Date updateTime;
}
