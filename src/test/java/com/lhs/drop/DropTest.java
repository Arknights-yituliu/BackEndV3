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
        Date start = sdf.parse("2026-02-22 00");
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);

        for (int i = 0; i < 10000; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
//            String tableName = "stage_drop_" + year + "_" + month;
            String tableName = "stage_drop_20251130_20260221";
            stageDropStatisticsService.stageDropHourlyStatistics(start, end, tableName);
            start = new Date(start.getTime() + oneHour);
            end = new Date(end.getTime() + oneHour);
        }
    }

    @Test
    public void testStageDropStatistics2() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        Date start = sdf.parse("2026-02-22 00");
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);

        for (int i = 0; i < 10000; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
//            String tableName = "stage_drop_" + year + "_" + month;
            String tableName = "stage_drop_20260222_20260328";
            stageDropStatisticsService.stageDropHourlyStatistics(start, end, tableName);
            start = new Date(start.getTime() + oneHour);
            end = new Date(end.getTime() + oneHour);
        }
    }

    @Test
    public void testStageDropStatistics3() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        Date start = sdf.parse("2026-02-22 00");
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);

        for (int i = 0; i < 10000; i++) {
            if(start.compareTo(sdf.parse("2026-04-01 19")) > 0){
                break;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
//            String tableName = "stage_drop_" + year + "_" + month;
            String tableName = "stage_drop_20260222_20260328";
            stageDropStatisticsService.stageDropHourlyStatistics(start, end, tableName);
            start = new Date(start.getTime() + oneHour);
            end = new Date(end.getTime() + oneHour);
        }
    }
}
