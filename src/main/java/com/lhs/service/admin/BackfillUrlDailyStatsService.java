package com.lhs.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.po.admin.AccessLogUrlDailyStatsTask;
import com.lhs.mapper.admin.AccessLogUrlDailyStatsTaskMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * URL每日访问量历史数据回填服务
 * 以固定起始日期（2026-08-05）为起点，每次调用向过去推进一天，
 * 将 2026-08-05 至 2024-01-01 之间的每天每个URL访问量逐日统计写入预聚合表。
 * 设计为被定时任务定时调用，每次统计一个"尚未统计"的日期，
 * 例如第一次统计 2026-08-05，第二次统计 2026-08-04，依此类推。
 * 回填游标持久化到 Redis，应用重启后从 Redis 恢复断点继续回填；
 * 已统计的日期会被自动跳过，因此本服务可重复调用、可随时中断、可安全续跑。
 */
@Service
public class BackfillUrlDailyStatsService {

    /** 每天的毫秒数 */
    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    /** 回填起始日期：2026-08-05（第一次执行统计该天） */
    private static final Date BACKFILL_START_DAY = buildDay(2026, 8, 5);

    /** 回填结束边界（不含）：2024-01-01，即最早回填到 2024-01-01 这天 */
    private static final Date BACKFILL_END_DAY = buildDay(2024, 1, 1);

    /** Redis key：回填游标，值为下次扫描起始日期（格式 yyyy-MM-dd） */
    private static final String BACKFILL_CURSOR_KEY = "BACKFILL:DAY:URL:ACCESS2:STATS:CURSOR";

    /** 游标日期格式化模板 */
    private static final String CURSOR_PATTERN = "yyyy-MM-dd";

    private final AccessService accessService;
    private final AccessLogUrlDailyStatsTaskMapper accessLogUrlDailyStatsTaskMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public BackfillUrlDailyStatsService(AccessService accessService,
                                        AccessLogUrlDailyStatsTaskMapper accessLogUrlDailyStatsTaskMapper,
                                        RedisTemplate<String, Object> redisTemplate) {
        this.accessService = accessService;
        this.accessLogUrlDailyStatsTaskMapper = accessLogUrlDailyStatsTaskMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 执行一次回填：从游标位置向过去方向查找第一个尚未统计的日期并统计它
     * 若该日期已在任务记录表中存在（正常定时任务或此前回填过），则自动跳过并继续向前查找
     * 当回填已推进到 2024-01-01 之前时，视为全部回填完成，不再执行
     *
     * @return 本次统计的日期（格式 yyyy-MM-dd），无需执行（已完成或无可统计日期）时返回 null
     */
    public String backfillOnce() {
        // 从 Redis 恢复游标
        Date cursor = loadCursor();

        // 已全部回填完成，直接返回
        if (cursor.getTime() < BACKFILL_END_DAY.getTime()) {
            return null;
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date day = cursor;

        // 向过去方向查找第一个尚未统计的日期
        while (day.getTime() >= BACKFILL_END_DAY.getTime()) {
            Long exists = accessLogUrlDailyStatsTaskMapper.selectCount(
                    new LambdaQueryWrapper<AccessLogUrlDailyStatsTask>()
                            .eq(AccessLogUrlDailyStatsTask::getStatDay, day)
            );
            if (exists == null || exists == 0L) {
                // 统计该天 [day, day + 1天)，随后游标向前推进一天并写回 Redis
                Date nextDay = new Date(day.getTime() + ONE_DAY);
                accessService.statisticsUrlDailyVisits(day, nextDay);
                saveCursor(new Date(day.getTime() - ONE_DAY));
                Logger.info("URL每日访问量回填：{} 统计完成", dayFormat.format(day));
                return dayFormat.format(day);
            }
            day = new Date(day.getTime() - ONE_DAY);
        }

        // 范围内全部已统计，标记回填完成
        saveCursor(BACKFILL_END_DAY);
        Logger.info("URL每日访问量回填：2026-08-05 至 2024-01-01 范围内全部日期均已统计，回填完成");
        return null;
    }

    /**
     * 从 Redis 读取回填游标
     * 无记录或记录格式异常时回退到起始日期，保证回填总能正常启动
     *
     * @return 下次扫描的起始日期
     */
    private Date loadCursor() {
        Object value = redisTemplate.opsForValue().get(BACKFILL_CURSOR_KEY);
        if (value == null) {
            return BACKFILL_START_DAY;
        }
        try {
            return new SimpleDateFormat(CURSOR_PATTERN).parse(String.valueOf(value));
        } catch (ParseException e) {
            Logger.error("URL每日访问量回填：游标 {} 解析失败，回退到起始日期", value);
            return BACKFILL_START_DAY;
        }
    }

    /**
     * 将回填游标写入 Redis
     *
     * @param cursor 下次扫描的起始日期
     */
    private void saveCursor(Date cursor) {
        redisTemplate.opsForValue().set(BACKFILL_CURSOR_KEY, new SimpleDateFormat(CURSOR_PATTERN).format(cursor));
    }

    /**
     * 构建指定日期的零点时间（分秒毫秒置零），用于定义回填起止边界
     *
     * @param year  年
     * @param month 月（1-12）
     * @param day   日
     * @return 该日 00:00:00.000 的时间
     */
    private static Date buildDay(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
