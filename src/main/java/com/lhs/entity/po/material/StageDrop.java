package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;



@TableName
public class StageDrop {
    @TableId
    private Long id;
    private String stageId;
    private String drops;
    private Integer times;
    private String server;
    private String source;
    private String uid;
    private String version;
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getDrops() {
        return drops;
    }

    public void setDrops(String drops) {
        this.drops = drops;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "StageDrop{" +
                "id=" + id +
                ", stageId='" + stageId + '\'' +
                ", drops='" + drops + '\'' +
                ", times=" + times +
                ", server='" + server + '\'' +
                ", source='" + source + '\'' +
                ", uid='" + uid + '\'' +
                ", version='" + version + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
