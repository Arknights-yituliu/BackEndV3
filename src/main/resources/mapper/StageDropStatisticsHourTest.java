package com.lhs.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.material.StageDropDetailDTO;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.entity.po.material.StageDropV2;
import com.lhs.mapper.material.StageDropMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@SpringBootTest
public class StageDropStatisticsHourTest {

    @Resource
    private StageDropMapper stageDropMapper;


    @Test
    void stageDropStatistics() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long start = 1720972800000L;
        for (int i = 0; i < 100000; i++) {
            long end = start + 60 * 60 * 1000;

            if(end>1722009600000L){
                return;
            }

            List<StageDropV2> stageDropV2List = stageDropMapper.selectStageDropV2ByDate("stage_drop_8",new Date(start), new Date(end));
            if (stageDropV2List.isEmpty()) {
                System.out.println("没有数据");
                start = end;
                continue;
            }

//            System.out.println("统计区间：" + sdf.format(start) + "——" + sdf.format(end));
            System.out.println("<————本次统计总数据量为————>：" + stageDropV2List.size());

            hourlyDataStatistics(stageDropV2List, new Date(start), new Date(end));



            start = end;

        }
    }

    private void hourlyDataStatistics(List<StageDropV2> stageDropV2List, Date start, Date end) {
        if (stageDropV2List.isEmpty()) {
            return;
        }
        IdGenerator idGenerator = new IdGenerator(1L);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        System.out.println("统计区间：" + sdf.format(stageDropV2List.get(0).getCreateTime()) + "——" +
                sdf.format(stageDropV2List.get(stageDropV2List.size() - 1).getCreateTime()));



        System.out.println("统计区间数据数量：" + stageDropV2List.size());

        HashMap<String, Integer> timesMap = new HashMap<>();
        HashMap<String, StageDropStatistics> dropQuantity = new HashMap<>();
        Date date = new Date();
        for (StageDropV2 stageDropV2 : stageDropV2List) {

            if (!"CN".equals(stageDropV2.getServer())) {
                continue;
            }

            String stageId = stageDropV2.getStageId();
            timesMap.merge(stageId, 1, Integer::sum);

            if (stageDropV2.getDrops()!=null&&stageDropV2.getDrops().length() > 5) {
                List<StageDropDetailDTO> stageDropDetailDTOList = JsonMapper.parseJSONArray(stageDropV2.getDrops(),
                        new TypeReference<>() {
                        });
                for (StageDropDetailDTO dropDetail : stageDropDetailDTOList) {
                    String itemId = dropDetail.getItemId();
                    String key = stageId + "&" + itemId;
                    Integer quantity = dropDetail.getQuantity();
                    if (dropQuantity.get(key) != null) {
                        dropQuantity.get(key).addQuantity(quantity);
                    } else {
                        StageDropStatistics stageDropStatistics = new StageDropStatistics();

                        stageDropStatistics.setStageId(stageId);
                        stageDropStatistics.setItemId(itemId);
                        stageDropStatistics.setQuantity(quantity);
                        stageDropStatistics.setStart(start);
                        stageDropStatistics.setEnd(end);
                        stageDropStatistics.setTimeGranularity(TimeGranularity.HOUR.code());
                        stageDropStatistics.setCreateTime(date);
                        dropQuantity.put(key, stageDropStatistics);
                    }
                }
            }
        }

        List<StageDropStatistics> insertList = new ArrayList<>();
        for (String key : dropQuantity.keySet()) {
            StageDropStatistics stageDropStatistics = dropQuantity.get(key);
            stageDropStatistics.setId(idGenerator.nextId());
            String stageId = stageDropStatistics.getStageId();
            stageDropStatistics.setTimes(timesMap.get(stageId));
            insertList.add(stageDropStatistics);
        }

        System.out.println(sdf.format(new Date()) + "——本次插入统计结果条数：" + insertList.size());
        if (insertList.isEmpty()) {
            return;
        }
        stageDropMapper.insertBatchStageDropStatistics(insertList);
    }
}
