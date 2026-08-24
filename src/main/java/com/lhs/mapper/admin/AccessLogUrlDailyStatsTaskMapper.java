package com.lhs.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.admin.AccessLogUrlDailyStatsTask;
import org.springframework.stereotype.Repository;

/**
 * URL每日访问量统计任务记录Mapper接口
 * 对应数据库表：access_log_url_daily_stats_task
 */
@Repository
public interface AccessLogUrlDailyStatsTaskMapper extends BaseMapper<AccessLogUrlDailyStatsTask> {
}
