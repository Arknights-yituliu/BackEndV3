package com.lhs.entity.vo.rogue;

import lombok.Data;

import java.util.Date;

@Data
public class RogueSeedRatingVO {
    private Long seedId;
    private Integer rating;
    private Date createTime;
}
