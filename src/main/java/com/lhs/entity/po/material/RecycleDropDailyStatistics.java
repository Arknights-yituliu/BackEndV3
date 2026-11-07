package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 回收站每日物品产出统计实体类
 * 对应数据库表 recycle_drop_daily_statistics
 * 同一天重跑会生成新 task_id 的记录并将旧记录置为 EXPIRE，查询仅读 DISPLAY 记录
 */
@TableName("recycle_drop_daily_statistics")
public class RecycleDropDailyStatistics {

    /** 主键ID，由 IdGenerator 生成 */
    @TableId
    private Long id;

    /** 统计的日期（当天 00:00:00） */
    private Date statDay;

    /** 物品 id */
    private String itemId;

    /** 当天该物品的产出总数量 */
    private Long quantity;

    /** 当天该物品的上报批次数 */
    private Long times;

    /** 本次统计的任务ID，关联 recycle_drop_daily_statistics_task.task_id */
    private Long taskId;

    /** 记录状态：1=展示数据(DISPLAY)，-1=过期数据(EXPIRE) */
    private Integer recordCode;

    /** 创建时间 */
    private Date createTime;

    public RecycleDropDailyStatistics() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStatDay() {
        return statDay;
    }

    public void setStatDay(Date statDay) {
        this.statDay = statDay;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getTimes() {
        return times;
    }

    public void setTimes(Long times) {
        this.times = times;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(Integer recordCode) {
        this.recordCode = recordCode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
