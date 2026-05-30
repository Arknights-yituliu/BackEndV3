package com.lhs.drop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.Logger;

import com.lhs.entity.dto.drop.StageDropTimesCountDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lhs.common.enums.TimeGranularity;
import com.lhs.entity.dto.drop.StageDropQuantityCountDTO;
import com.lhs.entity.po.material.StageDropStatistics;
import com.lhs.entity.vo.drop.StageDropStatisticsResultVO;
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
    public void testSelect() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String itemUpdateJsonText = FileUtil.read(ConfigUtil.DataFilePath + "item_update.json");
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

        // 收集每个关卡在每个小时的执行次数
        // key: stageId + "_" + 小时时间戳, value: 该小时该关卡的最大times
        Map<String, StageDropTimesCountDTO> stageHourTimes = new HashMap<>();

        // 按 stageId + itemId 聚合数量和时间范围
        Map<String, StageDropQuantityCountDTO> quantityCollectMap = new HashMap<>();

        while (start.before(now)) {
            cal.setTime(start);
            cal.add(Calendar.MONTH, 1);
            Date end = cal.getTime();

            List<StageDropStatistics> list = stageDropStatisticsMapper.selectListByDate(
                    TimeGranularity.HOUR.code(), start, end);
            System.out.println(sdf.format(start) + " ~ " + sdf.format(end) + " 数据量: " + list.size());

            for (StageDropStatistics item : list) {
                String stageId = item.getStageId();
                Long hourTimestamp = item.getStartTime().getTime();

                // 收集该时段该关卡的times（取最大值，同一关卡同一时段可能有多条材料记录）
                String stageTimeKey = stageId + "." + hourTimestamp;

                if(!stageHourTimes.containsKey(stageTimeKey)){
                    stageHourTimes.put(stageTimeKey,new StageDropTimesCountDTO(stageId, item.getItemId(), 0L));
                }
                StageDropTimesCountDTO stageDropTimesCountDTO = stageHourTimes.get(stageTimeKey);
                if(stageDropTimesCountDTO.getTimes()<item.getTimes()){
                    stageDropTimesCountDTO.setTimes(item.getTimes());
                }


                // 材料上架时间过滤：上架时间之前不统计
                String itemId = item.getItemId();
                Date updateTime = itemUpdateMap.get(itemId);
                if (updateTime != null && hourTimestamp < updateTime.getTime()) {
                    continue;
                }

                // 按 stageId + itemId 聚合 quantity
                String key = stageId + "_" + itemId;
                StageDropQuantityCountDTO quantityCount = quantityCollectMap.get(key);
                if (quantityCount == null) {
                    quantityCount = new StageDropQuantityCountDTO(stageId, itemId,
                            item.getStartTime(), item.getEndTime(), 0L);
                    quantityCollectMap.put(key, quantityCount);
                }
                quantityCount.addQuantity(item.getQuantity());

                // 追踪该材料的时间范围（精确到小时）
                if (quantityCount.getStart().getTime() > hourTimestamp) {
                    quantityCount.setStart(new Date(hourTimestamp));
                }
                if (quantityCount.getEnd().getTime() < item.getEndTime().getTime()) {
                    quantityCount.setEnd(item.getEndTime());
                }
            }
            start = end;
        }
        System.out.println(stageHourTimes.size());
        System.out.println(quantityCollectMap.size());

        // 将 stageHourTimes 按 stageId 预分组，避免结果组装时 O(items * hours) 全量遍历
        Map<String, List<Map.Entry<String, StageDropTimesCountDTO>>> stageHourGroup = new HashMap<>();
        for (Map.Entry<String, StageDropTimesCountDTO> hourEntry : stageHourTimes.entrySet()) {
            String key = hourEntry.getKey();
            int dotIdx = key.indexOf('.');
            String stageId = key.substring(0, dotIdx);
            stageHourGroup.computeIfAbsent(stageId, k -> new ArrayList<>()).add(hourEntry);
        }

        // 结果组装：times 从 stageHourTimes 中按材料时间窗统一获取
        List<StageDropStatisticsResultVO> result = new ArrayList<>();
        for (Map.Entry<String, StageDropQuantityCountDTO> entry : quantityCollectMap.entrySet()) {
            StageDropQuantityCountDTO count = entry.getValue();
            String stageId = count.getStageId();
            String itemId = count.getItemId();
            Date updateTime = itemUpdateMap.get(itemId);

            // 直接从预分组中获取该关卡的小时数据，只遍历该关卡的 hours
            long effectiveTimes = 0L;
            List<Map.Entry<String, StageDropTimesCountDTO>> stageHours = stageHourGroup.get(stageId);
            if (stageHours != null) {
                for (Map.Entry<String, StageDropTimesCountDTO> hourEntry : stageHours) {
                    String key = hourEntry.getKey();
                    long hourTimestamp = Long.parseLong(key.substring(key.indexOf('.') + 1));
                    if (updateTime == null || hourTimestamp >= updateTime.getTime()) {
                        if(stageId.equals("tough_14-11")&&itemId.equals("31113")){
                            System.out.println(sdf2.format(new Date(hourTimestamp)) + "  "+hourEntry.getValue().getTimes() );
                        }
                        effectiveTimes += hourEntry.getValue().getTimes();
                    }
                }
            }

            StageDropStatisticsResultVO item = new StageDropStatisticsResultVO();
            item.setStageId(stageId);
            item.setItemId(itemId);
            item.setQuantity(count.getQuantity());
            item.setTimes(effectiveTimes);
            item.setStart(count.getStart().getTime());
            item.setEnd(count.getEnd().getTime());
            result.add(item);
        }

        FileUtil.saveJsonFile("D:\\stageDrop\\export\\", "stage_drop.json",
                JsonMapper.toJSONString(result));
    }

    @Test
    void getStageDropByStageId() {
        String read = FileUtil.read("D:\\stageDrop\\export\\stage_drop.json");
        List<StageDropStatisticsResultVO> list = JsonMapper.parseObject(read, new TypeReference<>() {
        });

        String itemInfoText = FileUtil.read(ConfigUtil.DataFilePath + "item_info.json");
        List<JsonNode> itemInfoList = JsonMapper.parseObject(itemInfoText, new TypeReference<>() {
        });
        Map<String, String> itemNameMap = new HashMap<>();
        for (JsonNode node : itemInfoList) {
            itemNameMap.put(node.get("itemId").asText(), node.get("itemName").asText());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (StageDropStatisticsResultVO item : list) {
            if (item.getStageId().equals("act43side_06")) {
                Logger.info(item.getItemId() + "材料名：" + itemNameMap.get(item.getItemId())
                        + "，start：" + sdf.format(new Date(item.getStart()))
                        + "，end：" + sdf.format(new Date(item.getEnd()))
                        + "，quantity：" + item.getQuantity()
                        + "，times：" + item.getTimes()
                        + "，掉率：" + item.getQuantity()*100.0/item.getTimes());
            }
        }
    }
}
