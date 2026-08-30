package com.lhs.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.util.Logger;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.entity.po.admin.AccessLogHourlyStatsTask;
import com.lhs.mapper.admin.AccessLogHourlyStatsTaskMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 小时访问量历史数据回填服务
 * 以固定起始小时（2026-08-05 00:00）为起点，每次调用向过去推进一个小时，
 * 将 2026-08-05 00:00 至 2024-01-01 00:00 之间的每小时访问量逐小时统计写入预聚合表。
 * 设计为被定时任务定时调用，每次统计一个"尚未统计"的小时，
 * 例如第一次统计 2026-08-05 00:00，第二次统计 2026-08-04 23:00，依此类推。
 * 回填游标持久化到 Redis，应用重启后从 Redis 恢复断点继续回填；
 * 已统计的小时会被自动跳过，因此本服务可重复调用、可随时中断、可安全续跑。
 */
@Service
public class BackfillHourlyAccessStatsService {

    /** 每小时的毫秒数 */
    private static final long ONE_HOUR = 60 * 60 * 1000L;

    /** 回填起始小时：2026-08-05 00:00（第一次执行统计该小时） */
    private static final Date BACKFILL_START_HOUR = buildHour(2026, 8, 5);

    /** 回填结束边界（不含）：2024-01-01 00:00，即最早回填到 2024-01-01 00:00 这个小时 */
    private static final Date BACKFILL_END_HOUR = buildHour(2024, 1, 1);

    /** 游标时间格式化模板 */
    private static final String CURSOR_PATTERN = "yyyy-MM-dd HH:00:00";

    private final AccessService accessService;
    private final AccessLogHourlyStatsTaskMapper accessLogHourlyStatsTaskMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public BackfillHourlyAccessStatsService(AccessService accessService,
                                            AccessLogHourlyStatsTaskMapper accessLogHourlyStatsTaskMapper,
                                            RedisTemplate<String, Object> redisTemplate) {
        this.accessService = accessService;
        this.accessLogHourlyStatsTaskMapper = accessLogHourlyStatsTaskMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 执行一次回填：从游标位置向过去方向查找第一个尚未统计的小时并统计它
     * 若该小时已在任务记录表中存在（正常定时任务或此前回填过），则自动跳过并继续向前查找
     * 当回填已推进到 2024-01-01 00:00 之前时，视为全部回填完成，不再执行
     *
     * @return 本次统计的小时（格式 yyyy-MM-dd HH:00:00），无需执行（已完成或无可统计小时）时返回 null
     */
    public String backfillOnce() {
        // 从 Redis 恢复游标
        Date cursor = loadCursor();

        // 已全部回填完成，直接返回
        if (cursor.getTime() < BACKFILL_END_HOUR.getTime()) {
            return null;
        }

        SimpleDateFormat hourFormat = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
        Date hour = cursor;

        // 向过去方向查找第一个尚未统计的小时
        while (hour.getTime() >= BACKFILL_END_HOUR.getTime()) {
            Long exists = accessLogHourlyStatsTaskMapper.selectCount(
                    new LambdaQueryWrapper<AccessLogHourlyStatsTask>()
                            .eq(AccessLogHourlyStatsTask::getStatHour, hour)
            );
            if (exists == null || exists == 0L) {
                // 统计该小时 [hour, hour + 1h)，随后游标向前推进一小时并写回 Redis
                Date nextHour = new Date(hour.getTime() + ONE_HOUR);
                accessService.statisticsLastHour(hour, nextHour);
                saveCursor(new Date(hour.getTime() - ONE_HOUR));
                Logger.info("小时访问量回填：{} 统计完成", hourFormat.format(hour));
                return hourFormat.format(hour);
            }
            hour = new Date(hour.getTime() - ONE_HOUR);
        }

        // 范围内全部已统计，标记回填完成
        saveCursor(BACKFILL_END_HOUR);
        Logger.info("小时访问量回填：2026-08-05 至 2024-01-01 范围内全部小时均已统计，回填完成");
        return null;
    }

    /**
     * 从 Redis 读取回填游标
     * 无记录或记录格式异常时回退到起始小时，保证回填总能正常启动
     *
     * @return 下次扫描的起始小时
     */
    private Date loadCursor() {
        Object value = redisTemplate.opsForValue().get(RedisKeyUtil.backfillHourlyCursor());
        if (value == null) {
            return BACKFILL_START_HOUR;
        }
        try {
            return new SimpleDateFormat(CURSOR_PATTERN).parse(String.valueOf(value));
        } catch (ParseException e) {
            Logger.error("小时访问量回填：游标 {} 解析失败，回退到起始小时", value);
            return BACKFILL_START_HOUR;
        }
    }

    /**
     * 将回填游标写入 Redis
     *
     * @param cursor 下次扫描的起始小时
     */
    private void saveCursor(Date cursor) {
        redisTemplate.opsForValue().set(RedisKeyUtil.backfillHourlyCursor(), new SimpleDateFormat(CURSOR_PATTERN).format(cursor));
    }

    /**
     * 构建指定日期的整点时间（分秒毫秒置零），用于定义回填起止边界
     *
     * @param year  年
     * @param month 月（1-12）
     * @param day   日
     * @return 该日 00:00:00.000 的时间
     */
    private static Date buildHour(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
