package com.lhs.drop;

import com.lhs.common.util.Logger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import com.lhs.mapper.material.StageDropStatisticsMapper;
import com.lhs.service.material.StageDropStatisticsService;

import jakarta.annotation.Resource;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SpringBootTest
public class DropHourStatisticsTest {

    @Resource
    private StageDropStatisticsService stageDropStatisticsService;

    @Resource
    private StageDropStatisticsMapper stageDropStatisticsMapper;

    @Test
    public void stageDropStatistics202501() throws Exception {
        stageDropStatisticsByDate("2025-01-29 00");
    }

    @Test
    public void stageDropStatistics202502() throws Exception {
        stageDropStatisticsByDate("2025-02-01 00");
    }

    @Test
    public void stageDropStatistics202503() throws Exception {
        stageDropStatisticsByDate("2025-03-01 00");
    }

    @Test
    public void stageDropStatistics202504() throws Exception {
        stageDropStatisticsByDate("2025-04-01 00");
    }

    @Test
    public void stageDropStatistics202505() throws Exception {
        stageDropStatisticsByDate("2025-05-01 00");
    }

    @Test
    public void stageDropStatistics202506() throws Exception {
        stageDropStatisticsByDate("2025-06-01 00");
    }

    @Test
    public void stageDropStatistics202507() throws Exception {
        stageDropStatisticsByDate("2025-07-01 00");
    }

    @Test
    public void stageDropStatistics202508() throws Exception {
        stageDropStatisticsByDate("2025-08-01 00");
    }

    @Test
    public void stageDropStatistics202509() throws Exception {
        stageDropStatisticsByDate("2025-09-01 00");
    }

    @Test
    public void stageDropStatistics202510() throws Exception {
        stageDropStatisticsByDate("2025-10-01 00");
    }

    @Test
    public void stageDropStatistics202511() throws Exception {
        stageDropStatisticsByDate("2025-11-01 00");
    }

    @Test
    public void stageDropStatistics202512() throws Exception {
        stageDropStatisticsByDate("2025-12-01 00");
    }

    @Test
    public void stageDropStatistics202601() throws Exception {
        stageDropStatisticsByDate("2026-01-01 00");
    }

    @Test
    public void stageDropStatistics202602() throws Exception {
        stageDropStatisticsByDate("2026-02-01 00");
    }

    @Test
    public void stageDropStatistics202603() throws Exception {
        stageDropStatisticsByDate("2026-03-01 00");
    }

    @Test
    public void stageDropStatistics202604() throws Exception {
        stageDropStatisticsByDate("2026-04-01 00");
    }

    @Test
    public void stageDropStatistics202605() throws Exception {
        stageDropStatisticsByDate("2026-05-01 00");
    }





    private void stageDropStatisticsByDate(String startDate) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        Date start = sdf.parse(startDate);
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        int startMonth = startCal.get(Calendar.MONTH);

        for (int i = 0; i < 5000; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            if (cal.get(Calendar.MONTH) != startMonth) {
                Logger.info("start已进入次月，退出循环");
                break;
            }
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
            String tableName = String.format("stage_drop_%d_%02d", year, month);
            // String tableName = "stage_drop_20251130_20260221";
            stageDropStatisticsService.stageDropHourlyStatisticsV2(start, end, tableName);
            start = new Date(start.getTime() + oneHour);
            end = new Date(end.getTime() + oneHour);
        }
    }
   
}
