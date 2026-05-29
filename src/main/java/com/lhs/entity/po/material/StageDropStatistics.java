package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName
public class StageDropStatistics {

    private Long id;
    private String stageId;
    private String itemId;
    private Long times;
    private Long quantity;
    private Date endTime;
    private Date startTime;
    private Integer timeGranularity;
    private Date createTime;

    private Integer recordCode;


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

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Long getTimes() {
        return times;
    }

    public void setTimes(Long times) {
        this.times = times;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getTimeGranularity() {
        return timeGranularity;
    }

    public void setTimeGranularity(Integer timeGranularity) {
        this.timeGranularity = timeGranularity;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void addTimes(Long times){
        this.times+=times;
    }

    public void addQuantity(Long quantity){
        this.quantity+=quantity;
    }

    public Integer getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(Integer recordCode) {
        this.recordCode = recordCode;
    }

    @Override
    public String toString() {
        return "StageDropStatistics{" +
                "id=" + id +
                ", stageId='" + stageId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", times=" + times +
                ", quantity=" + quantity +
                ", statisticalPeriod=" + endTime +
                ", timeGranularity=" + timeGranularity +
                ", createTime=" + createTime +
                '}';
    }
}
