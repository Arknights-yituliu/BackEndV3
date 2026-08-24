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
 * URL每日访问量统计测试
 * 用于回填/刷新最近7天的每个URL每日访问量统计数据
 */
@SpringBootTest
public class AccessUrlDailyStatsTest {

    /** 每天毫秒数 */
    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    @Resource
    private AccessService accessService;

    /**
     * 回填最近7天的URL每日访问量统计
     * 从7天前的00:00开始，逐天调用 statisticsUrlDailyVisits 直至昨天
     * 重跑会按任务记录表流程：旧数据标记过期，生成新 task_id 写入最新统计
     */
    @Test
    public void statisticsLast7Days() {
        // 计算统计窗口：从7天前的00:00到昨天00:00，共7天
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();              // 今天00:00
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Date endDay = cal.getTime();             // 昨天00:00 = 回填的结束边界
        cal.add(Calendar.DAY_OF_YEAR, -6);
        Date startDay = cal.getTime();           // 7天前的00:00 = 回填的起始日

        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

        Logger.info("开始回填最近7天URL每日访问量：{} 至 {}", dayFormat.format(startDay), dayFormat.format(endDay));

        long totalUrls = 0;
        int dayCount = 0;
        Date day = startDay;
        while (day.getTime() <= endDay.getTime()) {
            Date nextDay = new Date(day.getTime() + ONE_DAY);
            long urlCount = accessService.statisticsUrlDailyVisits(day, nextDay);
            totalUrls += urlCount;
            dayCount++;
            Logger.info("{} 统计完成，共 {} 个URL", dayFormat.format(day), urlCount);
            day = nextDay;
        }

        Logger.info("最近7天URL每日访问量回填完成，共 {} 天，累计 {} 个URL记录", dayCount, totalUrls);
    }
}
