package com.lhs.admin;

import com.lhs.common.util.Logger;
import com.lhs.service.admin.AccessService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 小时访问量统计测试
 * 用于回填/刷新最近7天的小时访问量统计数据
 */
@SpringBootTest
public class AccessHourlyStatsTest {

    /** 每小时毫秒数 */
    private static final long ONE_HOUR = 60 * 60 * 1000L;

    @Resource
    private AccessService accessService;

    /**
     * 跑最近7天的小时访问量统计
     * 从7天前的整点开始，逐小时调用 statisticsLastHour 直至当前整点
     * 重跑会按任务记录表流程：旧数据标记过期，生成新 task_id 写入最新统计
     */
    @Test
    public void statisticsLast7Days() {
        // 计算统计窗口：[7天前的整点, 当前整点)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date endBoundary = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date start = cal.getTime();

        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat hourFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Logger.info("开始统计最近7天访问量：{} 至 {}", hourFormat.format(start), hourFormat.format(endBoundary));

        long total = 0;        // 7天累计访问量
        long dayTotal = 0;     // 当天累计访问量
        int hourCount = 0;     // 当天已统计小时数
        String currentDay = dayFormat.format(start);

        Date end = new Date(start.getTime() + ONE_HOUR);
        while (end.getTime() <= endBoundary.getTime()) {
            long visitCount = accessService.statisticsLastHour(start, end);
            total += visitCount;
            dayTotal += visitCount;
            hourCount++;

            String day = dayFormat.format(start);
            if (!day.equals(currentDay)) {
                Logger.info("{} 访问量合计：{}（{}个小时）", currentDay, dayTotal, hourCount);
                currentDay = day;
                dayTotal = 0;
                hourCount = 0;
            }

            start = end;
            end = new Date(end.getTime() + ONE_HOUR);
        }

        Logger.info("{} 访问量合计：{}（{}个小时）", currentDay, dayTotal, hourCount);
        Logger.info("最近7天访问量统计完成，总计：{}", total);
    }
}
