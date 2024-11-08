package com.lhs.entity.po.rogueSeed;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName
public class RogueSeedRatingStatistics {
    @TableId
    private Long seedId;
    private Integer dislikeCount = 0;
    private Integer likeCount = 0;
    private Integer normalCount = 0;

    {
        dislikeCount = 0;
        likeCount = 0;
        normalCount = 0;
    }
}
