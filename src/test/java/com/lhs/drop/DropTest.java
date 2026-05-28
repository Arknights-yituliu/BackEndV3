package com.lhs.drop;

import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lhs.common.enums.TimeGranularity;
import com.lhs.entity.dto.drop.StageDropCount;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.mapper.material.StageDropStatisticsMapper;
import com.lhs.service.material.StageDropStatisticsService;

import jakarta.annotation.Resource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class DropTest {

    @Resource
    private StageDropStatisticsService stageDropStatisticsService;

    @Resource
    private StageDropStatisticsMapper stageDropStatisticsMapper;

    @Test
    public void testStageDropStatistics() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        Date start = sdf.parse("2026-05-01 00");
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);

        for (int i = 0; i < 1000; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
            String tableName = String.format("stage_drop_%d_%02d", year, month);
            // String tableName = "stage_drop_20251130_20260221";
            stageDropStatisticsService.stageDropHourlyStatistics(start, end, tableName);
            start = new Date(start.getTime() + oneHour);
            end = new Date(end.getTime() + oneHour);
        }
    }

    @Test
    public void testSelect() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date start = sdf.parse("2025-01-01");
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        Date now = new Date();

        Map<String, StageDropCount> collect = new HashMap<>();
        List<StageDropStatistics> list = new ArrayList<>();
        while (start.before(now)) {
            cal.setTime(start);
            cal.add(Calendar.MONTH, 1);
            Date end = cal.getTime();

            list = stageDropStatisticsMapper.listByDate(
                    TimeGranularity.HOUR.code(), start, end);
            System.out.println(sdf.format(start) + " ~ " + sdf.format(end) + " 数据量: " + list.size());
            for (StageDropStatistics item : list) {
                String key = item.getStageId() + "_" + item.getItemId();
                StageDropCount count = collect.getOrDefault(key, new StageDropCount(item.getStageId(),item.getItemId(),0,0));
                count.addQuantity(item.getQuantity());
                count.addTimes(item.getTimes());
                collect.put(key, count);
            }
            start = end;
        }
        
        List<StageDropStatistics> result = new ArrayList<>();
        for(Map.Entry<String,StageDropCount> entry: collect.entrySet()){
            String key = entry.getKey();
            StageDropCount count = entry.getValue();

            StageDropStatistics item = new StageDropStatistics();
            item.setStageId(count.getStageId());
            item.setItemId(count.getItemId());
            item.setQuantity(count.getQuantity());
            item.setTimes(count.getTimes());
            result.add(item);
        }

        FileUtil.saveJsonFile("C:\\Users\\admin\\Desktop\\import\\", "stage_drop.json", JsonMapper.toJSONString(result));
        
        StageDropCount stageDropCount = collect.get("main_01-07_30012");
        Logger.info(String.valueOf(stageDropCount.getQuantity()*1.0/stageDropCount.getTimes()));

    }
}
