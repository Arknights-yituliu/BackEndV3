package com.lhs.entity.vo.rogueSeed;

import lombok.Data;

import java.util.Date;

@Data
public class RogueSeedRatingVO {
    private Long ratingId;
    private Long seedId;
    private Integer rating;
    private Date createTime;
}
