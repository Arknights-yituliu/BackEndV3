package com.lhs.drop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;

import com.lhs.entity.dto.drop.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDrop;
import com.lhs.mapper.material.StageDropMapper;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootTest
public class StageDropActVersionCheckTest {

    @Resource
    private StageDropMapper stageDropMapper;

    /**
     * 检查关卡掉率原始数据中，drops 里 itemId 含 'act' 但 quantity 不是 times 的 3 倍数的记录，
     * 收集其版本号并打印（遍历2025年全年）
     */
    @Test
    public void checkActVersion() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH");
        long oneHour = 1000L * 60 * 60;

        Set<String> invalidVersions = new HashSet<>();

        // 逐月遍历2025年
        for (int month = 1; month <= 12; month++) {
            String startText = String.format("2025-%02d-01 00", month);
            Date start = sdf.parse(startText);
            int startMonth = month - 1; // Calendar.MONTH 是 0-based

            String tableName = String.format("stage_drop_2025_%02d", month);
            Logger.info("开始处理 " + tableName);

            for (int i = 0; i < 5000; i++) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(start);
                if (cal.get(Calendar.MONTH) != startMonth) {
                    break;
                }

                Date end = new Date(start.getTime() + oneHour);

                List<StageDrop> stageDropList = stageDropMapper.selectListByDate(tableName, start, end);

                for (StageDrop stageDrop : stageDropList) {
                    if (stageDrop.getTimes() == null || stageDrop.getTimes() == 0) {
                        continue;
                    }
                    String dropsJson = stageDrop.getDrops();
                    if (dropsJson == null || dropsJson.isEmpty() || "[]".equals(dropsJson)) {
                        continue;
                    }
                    List<StageDropDetailDTO> drops = JsonMapper.parseObject(dropsJson, new TypeReference<>() {});
                    if (drops == null) {
                        continue;
                    }
                    for (StageDropDetailDTO drop : drops) {
                        if (drop.getItemId() != null && drop.getItemId().contains("act")
                                && drop.getQuantity() != null) {
                            // 判断 quantity 除以 times 的结果是否为 3 的倍数
                            if (drop.getQuantity() % stageDrop.getTimes() == 0
                                    && (drop.getQuantity() / stageDrop.getTimes()) % 3 != 0) {
                                invalidVersions.add(stageDrop.getVersion());
                            }
                        }
                    }
                }

                start = new Date(start.getTime() + oneHour);
            }
        }

        System.out.println("========================================");
        System.out.println("itemId含'act'且quantity/times不是3的倍数的版本号：");
        for (String version : invalidVersions) {
            System.out.println("  " + version);
        }
        System.out.println("共 " + invalidVersions.size() + " 个版本号");
        System.out.println("========================================");
    }

}
