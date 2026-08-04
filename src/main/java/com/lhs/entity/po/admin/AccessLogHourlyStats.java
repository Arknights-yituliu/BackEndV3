package com.lhs.entity.po.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 每小时访问量统计实体类
 * 对应数据库表 access_log_hourly_stats
 * 同一小时重跑会生成新 task_id 的记录并将旧记录置为 EXPIRE，API 仅读 DISPLAY 记录
 */
@TableName("access_log_hourly_stats")
public class AccessLogHourlyStats {

    /** 主键ID，由 IdGenerator 生成 */
    @TableId
    private Long id;

    /** 统计的小时（整点，例如 2026-08-04 14:00:00） */
    private Date statHour;

    /** 该小时的总访问量 */
    private Long visitCount;

    /** 本次统计的任务ID，关联 access_log_hourly_stats_task.task_id */
    private Long taskId;

    /** 记录状态：1=展示数据(DISPLAY)，-1=过期数据(EXPIRE) */
    private Integer recordCode;

    /** 创建时间 */
    private Date createTime;

    public AccessLogHourlyStats() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStatHour() {
        return statHour;
    }

    public void setStatHour(Date statHour) {
        this.statHour = statHour;
    }

    public Long getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(Long visitCount) {
        this.visitCount = visitCount;
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
