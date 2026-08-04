package com.lhs.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.admin.AccessLogUrlDailyStats;
import org.springframework.stereotype.Repository;

/**
 * URL每日访问量统计Mapper接口
 * 对应数据库表：access_log_url_daily_stats
 */
@Repository
public interface AccessLogUrlDailyStatsMapper extends BaseMapper<AccessLogUrlDailyStats> {
}
