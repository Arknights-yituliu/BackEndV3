package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName
public class StageDropStatistics {

    private Long id;
    private String stageId;
    private String itemId;
    private Integer times;
    private Integer quantity;
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

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
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

    public void addTimes(Integer times){
        this.times+=times;
    }

    public void addQuantity(Integer quantity){
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
