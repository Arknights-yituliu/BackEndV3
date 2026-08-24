package com.lhs.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;
import com.lhs.entity.po.maa.Schedule;
import com.lhs.mapper.ScheduleMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Schedule 表数据导出测试
 * 从最新记录开始往前导出 5 万条，每 2000 条一个文件
 * 分页查询，避免 OOM
 */
@SpringBootTest
public class ScheduleExportTest {

    /** 导出目录 */
    private static final String EXPORT_DIR = "D:\\schedule_export\\";

    /** 总导出条数 */
    private static final int TOTAL_LIMIT = 50000;

    /** 每次查询条数 */
    private static final int PAGE_SIZE = 2000;

    @Resource
    private ScheduleMapper scheduleMapper;

    /**
     * 导出 schedule 表数据：最新 5 万条，每 2000 条为一个 JSON 文件
     * 使用 LIMIT offset,size 分批查询，每次只查 2000 条，避免因单条 JSON 太大导致 OOM
     */
    @Test
    void exportScheduleData() {
        long startTime = System.currentTimeMillis();
        Logger.info("=== 开始导出 schedule 表数据，总量 {} 条，每次查询 {} 条 ===", TOTAL_LIMIT, PAGE_SIZE);

        int totalExported = 0;
        int fileIndex = 0;

        for (int offset = 0; offset < TOTAL_LIMIT; offset += PAGE_SIZE) {
            // 分批查询，按创建时间倒序，每次 2000 条
            List<Schedule> batch = scheduleMapper.selectList(
                    new LambdaQueryWrapper<Schedule>()
                            .orderByDesc(Schedule::getCreateTime)
                            .last("LIMIT " + offset + ", " + PAGE_SIZE)
            );

            if (batch.isEmpty()) {
                Logger.info("offset={} 无数据，导出结束", offset);
                break;
            }

            fileIndex++;
            totalExported += batch.size();

            // 文件命名含序号和 UID 范围
            String recordRange = (batch.size() == 1)
                    ? String.valueOf(batch.get(0).getUid())
                    : batch.get(0).getUid() + "_" + batch.get(batch.size() - 1).getUid();
            String fileName = String.format("schedule_part%02d_%d条_uid_%s.json",
                    fileIndex, batch.size(), recordRange);

            String json = JsonMapper.toJSONString(batch);
            FileUtil.saveJsonFile(EXPORT_DIR, fileName, json);
            Logger.info("已导出文件 {}: {} ({} 条, {} 字节)",
                    fileIndex, fileName, batch.size(), json.getBytes().length);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        Logger.info("=== 导出完成：{} 条记录 → {} 个文件，耗时 {} ms ===",
                totalExported, fileIndex, elapsed);
    }
}
