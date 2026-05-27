package com.lhs.drop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lhs.service.material.StageDropStatisticsService;

import jakarta.annotation.Resource;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SpringBootTest
public class DropTest  {

    @Resource
    private StageDropStatisticsService stageDropStatisticsService;

    @Test
    public void testStageDropStatistics() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        Date start = sdf.parse("2026-01-01 00");
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);

        for (int i = 0; i < 100000; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
//            String tableName = "stage_drop_" + year + "_" + month;
            String tableName = "stage_drop_20260102_20260221";
            stageDropStatisticsService.stageDropHourlyStatistics(start, end, tableName);
            start = new Date(start.getTime() + oneHour);
            end = new Date(end.getTime() + oneHour);
        }
    }
}
