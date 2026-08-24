package com.lhs.entity.po.admin;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 小时访问量统计任务记录实体类
 * 对应数据库表 access_log_hourly_stats_task
 * 记录每个统计小时的执行情况与当前有效 task_id，用于判断当前小时是否已统计及重跑时定位旧数据
 */
@TableName("access_log_hourly_stats_task")
public class AccessLogHourlyStatsTask {

    /** 主键ID，由 IdGenerator 生成 */
    @TableId
    private Long id;

    /** 统计的小时（整点） */
    private Date statHour;

    /** 该小时当前有效的任务ID，关联 access_log_hourly_stats.task_id */
    private Long taskId;

    /** 该小时的访问量 */
    private Long dataCount;

    /** 首次创建时间 */
    private Date createTime;

    /** 最近一次重新统计时间 */
    private Date updateTime;

    public AccessLogHourlyStatsTask() {
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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getDataCount() {
        return dataCount;
    }

    public void setDataCount(Long dataCount) {
        this.dataCount = dataCount;
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
}
