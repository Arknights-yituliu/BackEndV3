package com.lhs.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.admin.AccessLogHourlyStatsTask;
import org.springframework.stereotype.Repository;

/**
 * 小时访问量统计任务记录Mapper接口
 * 对应数据库表：access_log_hourly_stats_task
 */
@Repository
public interface AccessLogHourlyStatsTaskMapper extends BaseMapper<AccessLogHourlyStatsTask> {
}
