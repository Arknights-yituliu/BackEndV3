package com.lhs.mapper.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lhs.entity.po.material.RecycleDropDailyStatisticsTask;
import org.springframework.stereotype.Repository;

/**
 * 回收站每日物品产出统计任务记录Mapper接口
 * 对应数据库表：recycle_drop_daily_statistics_task
 */
@Repository
public interface RecycleDropDailyStatisticsTaskMapper extends BaseMapper<RecycleDropDailyStatisticsTask> {
}
