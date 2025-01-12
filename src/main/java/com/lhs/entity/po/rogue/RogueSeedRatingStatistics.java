package com.lhs.entity.po.rogue;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class RogueSeedRatingStatistics {
    @TableId
    private Long seedId;
    private Double rating;
    private Integer ratingCount;

    {
        rating = 0.0;
        ratingCount = 0;
    }
}
