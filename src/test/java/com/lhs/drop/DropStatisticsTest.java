package com.lhs.drop;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;

import org.checkerframework.checker.units.qual.s;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lhs.common.enums.TimeGranularity;
import com.lhs.entity.dto.drop.StageDropQuantityDTO;
import com.lhs.entity.dto.drop.StageDropTimesDTO;
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
public class DropStatisticsTest {

    @Resource
    private StageDropStatisticsService stageDropStatisticsService;

    @Resource
    private StageDropStatisticsMapper stageDropStatisticsMapper;

    @Test
    public void stageDropStatistics1() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        Date start = sdf.parse("2026-01-01 00");
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);

        for (int i = 0; i < 10000; i++) {
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
    public void stageDropStatistics2() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        Date start = sdf.parse("2025-01-01 00");
        Date deadline = sdf.parse("2025-06-02 00");
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);
        for (int i = 0; i < 10000; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
            String tableName = String.format("stage_drop_%d_%02d", year, month);
            // String tableName = "stage_drop_20251130_20260221";

            if (start.after(deadline)) {
                Logger.info("start大于2025-06-02 00，退出循环");
                break;
            }

            stageDropStatisticsService.stageDropHourlyStatistics(start, end, tableName);
            start = new Date(start.getTime() + oneHour);
            end = new Date(end.getTime() + oneHour);
        }
    }

    @Test
    public void stageDropStatistics3() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        Date start = sdf.parse("2025-06-01 00");
        Date deadline = sdf.parse("2026-01-02 00");
        long oneHour = 1000 * 60 * 60;
        Date end = new Date(start.getTime() + oneHour);

        for (int i = 0; i < 10000; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            int month = cal.get(Calendar.MONTH) + 1;
            int year = cal.get(Calendar.YEAR);
            String tableName = String.format("stage_drop_%d_%02d", year, month);
            // String tableName = "stage_drop_20251130_20260221";

            if (start.after(deadline)) {
                Logger.info("start大于2025-06-02 00，退出循环");
                break;
            }

            stageDropStatisticsService.stageDropHourlyStatistics(start, end, tableName);
            start = new Date(start.getTime() + oneHour);
            end = new Date(end.getTime() + oneHour);
        }
    }

    @Test
    public void testSelect() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String itemUpdateJsonText = FileUtil.read(ConfigUtil.DataFilePath + "item_update");
        JsonNode itemUpdateTime = JsonMapper.parseJSONObject(itemUpdateJsonText);
        Map<String, Date> itemUpdateMap = new HashMap<>();
        for (JsonNode jsonNode : itemUpdateTime) {
            String itemId = jsonNode.get("itemId").asText();
            Date updateTime = sdf.parse(jsonNode.get("updateTime").asText());
            itemUpdateMap.put(itemId, updateTime);
        }

        Date start = sdf.parse("2025-01-01");
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        Date now = new Date();

        Map<String, StageDropQuantityDTO> quantityCollectMap = new HashMap<>();
        Map<String, StageDropTimesDTO> timesCollectMap = new HashMap<>();
        List<StageDropStatistics> list;

        while (start.before(now)) {
            cal.setTime(start);
            cal.add(Calendar.MONTH, 1);
            Date end = cal.getTime();

            list = stageDropStatisticsMapper.selectListByDate(
                    TimeGranularity.HOUR.code(), start, end);
            System.out.println(sdf.format(start) + " ~ " + sdf.format(end) + " 数据量: " + list.size());
            for (StageDropStatistics item : list) {
                String itemId = item.getItemId();
                Date updateTime = itemUpdateMap.get(itemId);
                if (updateTime != null) {
                    if(updateTime.before(start)){
                        continue;
                    }
                }
                String key = item.getStageId() + "_" + item.getItemId();
                StageDropQuantityDTO quantityCount = quantityCollectMap.getOrDefault(key,
                        new StageDropQuantityDTO(item.getStageId(), item.getItemId(), start, end, 0L));
                quantityCount.addQuantity(item.getQuantity());
                if(quantityCount.getStart().after(start)){
                    quantityCount.setStart(start);
                }
                if(quantityCount.getEnd().before(end)){
                    quantityCount.setEnd(end);
                }
                StageDropTimesDTO timesCount = timesCollectMap.getOrDefault(key,
                        new StageDropTimesDTO(item.getStageId(), item.getItemId(), 0L));
                timesCount.addTimes(item.getTimes());
                timesCollectMap.put(key, timesCount);

                quantityCollectMap.put(key, quantityCount);
                timesCollectMap.put(key, timesCount);
            }
            start = end;
        }

        List<StageDropStatistics> result = new ArrayList<>();
        for (Map.Entry<String, StageDropQuantityDTO> entry : quantityCollectMap.entrySet()) {
            String key = entry.getKey();
            StageDropQuantityDTO count = entry.getValue();
            StageDropTimesDTO timesCount = timesCollectMap.get(key);

            StageDropStatistics item = new StageDropStatistics();
            item.setStageId(count.getStageId());
            item.setItemId(count.getItemId());
            item.setQuantity(count.getQuantity());

            result.add(item);
        }

        FileUtil.saveJsonFile("C:\\Users\\admin\\Desktop\\import\\", "stage_drop.json",
                JsonMapper.toJSONString(result));

    }
}
