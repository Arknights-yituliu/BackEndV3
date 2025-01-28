package com.lhs.entity.po.survey;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName
public class OperatorCarryRateStatistics {
    @TableId
    private Long id;
    private String charId;
    private Double carryingRate;
    private Date createTime;
    private Boolean expiredFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCharId() {
        return charId;
    }

    public void setCharId(String charId) {
        this.charId = charId;
    }

    public Double getCarryingRate() {
        return carryingRate;
    }

    public void setCarryingRate(Double carryingRate) {
        this.carryingRate = carryingRate;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Boolean getExpiredFlag() {
        return expiredFlag;
    }

    public void setExpiredFlag(Boolean expiredFlag) {
        this.expiredFlag = expiredFlag;
    }
}
