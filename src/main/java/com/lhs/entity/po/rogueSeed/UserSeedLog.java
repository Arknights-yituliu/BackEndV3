package com.lhs.entity.po.rogueSeed;

import lombok.Data;

import java.util.Date;


public class UserSeedLog {
    private Long id;
    private Long uid;
    private Long seed;
    private Date createTime;
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "UserSeedLog{" +
                "id=" + id +
                ", uid=" + uid +
                ", seed=" + seed +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
