package com.lhs.entity.po.rogue;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;


@TableName
public class RogueSeedRatingStatistics {
    @TableId
    private Long id;
    private Long seedId;
    private Integer seedType;
    private Double rating;
    private Integer ratingCount;
    private Date createTime;
    private Boolean deleteFlag;


    {
        rating = 0.0;
        ratingCount = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSeedId() {
        return seedId;
    }

    public void setSeedId(Long seedId) {
        this.seedId = seedId;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Boolean getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public Integer getSeedType() {
        return seedType;
    }

    public void setSeedType(Integer seedType) {
        this.seedType = seedType;
    }

    @Override
    public String toString() {
        return "RogueSeedRatingStatistics{" +
                "id=" + id +
                ", seedId=" + seedId +
                ", seedType=" + seedType +
                ", rating=" + rating +
                ", ratingCount=" + ratingCount +
                ", createTime=" + createTime +
                ", deleteFlag=" + deleteFlag +
                '}';
    }
}
