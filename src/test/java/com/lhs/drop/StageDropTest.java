package com.lhs.drop;

import com.lhs.common.util.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.mapper.material.StageDropMapper;

import jakarta.annotation.Resource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SpringBootTest
public class StageDropTest {

    @Resource
    private StageDropMapper stageDropMapper;

  

    @Test
    void importStageDrop() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date start = sdf.parse("2026-05-01 00:00:00");
        long oneDay = 1000 * 60 * 60 * 24;
        Date end = new Date(start.getTime() + oneDay);
        for (int i = 0; i < 100; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            if (cal.get(Calendar.MONTH) == Calendar.JUNE) {
                 Logger.info("start已进入六月，退出循环");
                break;
            }
//            Date deadline = sdf.parse("2025-02-10 00:00:00");
//            if (start.after(deadline)) {
//                Logger.info("start大于2025-02-10，退出循环");
//                break;
//            }

            List<StageDrop> list = stageDropMapper.listOldStageDropByDate("stage_drop_20260329_20260512", start, end);

            Logger.info("查询日期：" + sdf.format(start) + "至" + sdf.format(end) + "，数量：" + list.size());

            start.setTime(start.getTime() + oneDay);
            end.setTime(end.getTime() + oneDay);
            if (list.isEmpty()) {
                continue;
            }

            int totalSize = list.size();
            int batchSize = 2000;
            for (int j = 0; j < totalSize; j += batchSize) {
                int endIdx = Math.min(j + batchSize, totalSize);
                List<StageDrop> chunk = list.subList(j, endIdx);
                stageDropMapper.insertBatchByTable("stage_drop_2026_05", chunk);
                Logger.info("已插入 " + chunk.size() + " 条，进度 " + endIdx + "/" + totalSize);
            }
        }

    }


        @Test
    void importStageDrop2() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date start = sdf.parse("2024-11-01 00:00:00");
        long oneDay = 1000 * 60 * 60 * 24;
        Date end = new Date(start.getTime() + oneDay);
        for (int i = 0; i < 100; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            if (cal.get(Calendar.MONTH) == Calendar.DECEMBER) {
                 Logger.info("start已进入十二月，退出循环");
                break;
            }
//            Date deadline = sdf.parse("2025-02-10 00:00:00");
//            if (start.after(deadline)) {
//                Logger.info("start大于2025-02-10，退出循环");
//                break;
//            }

            List<StageDrop> list = stageDropMapper.listOldStageDropByDate("stage_drop", start, end);

            Logger.info("查询日期：" + sdf.format(start) + "至" + sdf.format(end) + "，数量：" + list.size());

            start.setTime(start.getTime() + oneDay);
            end.setTime(end.getTime() + oneDay);
            if (list.isEmpty()) {
                continue;
            }

            int totalSize = list.size();
            int batchSize = 2000;
            for (int j = 0; j < totalSize; j += batchSize) {
                int endIdx = Math.min(j + batchSize, totalSize);
                List<StageDrop> chunk = list.subList(j, endIdx);
                stageDropMapper.insertBatchByTable("stage_drop_2024_11", chunk);
                Logger.info("已插入 " + chunk.size() + " 条，进度 " + endIdx + "/" + totalSize);
            }
        }

    }
}
