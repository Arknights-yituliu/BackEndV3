package com.lhs.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.admin.AccessLogHourlyStats;
import org.springframework.stereotype.Repository;

/**
 * 每小时访问量统计Mapper接口
 * 对应数据库表：access_log_hourly_stats
 */
@Repository
public interface AccessLogHourlyStatsMapper extends BaseMapper<AccessLogHourlyStats> {
}
