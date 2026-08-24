package com.lhs.drop;

import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.Logger;
import com.lhs.entity.po.material.StageDropStatisticsTaskLog;
import com.lhs.mapper.material.StageDropHourStatisticsMapper;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootTest
public class DropStatAppendTest {

    @Resource
    private StageDropHourStatisticsMapper stageDropHourStatisticsMapper;

    @Test
    public void test202501() throws Exception {
        appendRecordId("2025-01-01 00:00:00");
    }

    @Test
    public void test202502() throws Exception {
        appendRecordId("2025-02-01 00:00:00");
    }

    @Test
    public void test202503() throws Exception {
        appendRecordId("2025-03-01 00:00:00");
    }

    @Test
    public void test202504() throws Exception {
        appendRecordId("2025-04-01 00:00:00");
    }

    @Test
    public void test202505() throws Exception {
        appendRecordId("2025-05-01 00:00:00");
    }

    @Test
    public void test202506() throws Exception {
        appendRecordId("2025-06-01 00:00:00");
    }

    @Test
    public void test202507() throws Exception {
        appendRecordId("2025-07-01 00:00:00");
    }

    @Test
    public void test202508() throws Exception {
        appendRecordId("2025-08-01 00:00:00");
    }

    @Test
    public void test202509() throws Exception {
        appendRecordId("2025-09-01 00:00:00");
    }

    @Test
    public void test202510() throws Exception {
        appendRecordId("2025-10-01 00:00:00");
    }

    @Test
    public void test202511() throws Exception {
        appendRecordId("2025-11-01 00:00:00");
    }

    @Test
    public void test202512() throws Exception {
        appendRecordId("2025-12-01 00:00:00");
    }

    @Test
    public void test202601() throws Exception {
        appendRecordId("2026-01-01 00:00:00");
    }

    @Test
    public void test202602() throws Exception {
        appendRecordId("2026-02-01 00:00:00");
    }

    @Test
    public void test202603() throws Exception {
        appendRecordId("2026-03-01 00:00:00");
    }

    @Test
    public void test202604() throws Exception {
        appendRecordId("2026-04-01 00:00:00");
    }

    @Test
    public void test202605() throws Exception {
        appendRecordId("2026-05-01 00:00:00");
    }

    @Test
    public void test202606() throws Exception {
        appendRecordId("2026-06-01 00:00:00");
    }

    /**
     * 从2024年4月1日起，逐小时将log的id作为recordId写入statistics "2024-05-19 19:00:00"
     * recordId直接复用StageDropStatisticsTaskLog的id，不再额外生成
     */
    @Test
    public void appendRecordId(String startText) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date start = sdf.parse(startText);
    
        long oneHour = 1000L * 60 * 60;

        int totalProcessed = 0;
        int totalSkipped = 0;
        int totalStatsUpdated = 0;
        int totalDuplicatesRemoved = 0;

        // 批量删除重复log，减少逐条DELETE的网络开销
        List<Long> duplicateIds = new ArrayList<>();

        for (int t = 0; t < 15000; t++) {
            
            Date end = new Date(start.getTime() + oneHour);
            
            Logger.info("开始处理时段：" + sdf.format(start) + " 至 " + sdf.format(end));
            // 查询该时间段的所有log（可能有多条重复）
            List<StageDropStatisticsTaskLog> taskLogs = stageDropHourStatisticsMapper
                    .listTaskLogByTimeGranularityAndDate(TimeGranularity.HOUR.code(), start, end);

            if (taskLogs.isEmpty()) {
                totalSkipped++;
                start = end;
                continue;
            }

            // 去重：保留最早的一条，删除其余
            StageDropStatisticsTaskLog oldTaskLog = taskLogs.get(0);
            if (taskLogs.size() > 1) {
                for (int i = 1; i < taskLogs.size(); i++) {
                    duplicateIds.add(taskLogs.get(i).getId());
                    totalDuplicatesRemoved++;
                }
            }

            // dataCount为空或0说明该时段无原始数据，跳过statistics更新
            if (oldTaskLog.getDataCount() == null || oldTaskLog.getDataCount() == 0) {
                totalSkipped++;
                start = end;
                continue;
            }

//            // 更新statistics的recordId，直接通过updated返回值判断是否有数据
//            Long recordId = oldTaskLog.getId();
//            int updated = stageDropStatisticsMapper
//                    .appendRecordIdByDate(recordId, TimeGranularity.HOUR.code(), start, end);
//
//            if (updated > 0) {
//                totalStatsUpdated += updated;
//                totalProcessed++;
//            } else {
//                totalSkipped++;
//            }
//
//            if (totalProcessed % 500 == 0) {
//                Logger.info("已处理 " + totalProcessed + " 个时段，更新statistics " + totalStatsUpdated + " 行，"
//                        + sdf.format(start) + "，最新recordId：" + recordId);
//            }

            start = end;
        }

        // 批量执行重复log的删除
        if (!duplicateIds.isEmpty()) {
            for (Long id : duplicateIds) {
                stageDropHourStatisticsMapper.deleteTaskLogById(id);
            }
        }

        Logger.info("处理完成：总时段 " + totalProcessed + "，跳过 " + totalSkipped
                + "，更新statistics " + totalStatsUpdated + " 行，删除重复log " + totalDuplicatesRemoved + " 条");
    }
}
