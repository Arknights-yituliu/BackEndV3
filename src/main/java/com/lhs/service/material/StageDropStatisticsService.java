package com.lhs.service.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.TimeGranularity;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.IdGenerator;

import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.drop.StageDropQuantityCountDTO;
import com.lhs.entity.dto.drop.StageDropTimesCountDTO;
import com.lhs.entity.po.material.StageDropHourStatistics;
import com.lhs.entity.vo.drop.StageDropStatisticsResultVO;
import com.lhs.mapper.material.StageDropHourStatisticsMapper;
import com.lhs.mapper.material.StageDropMapper;
import com.lhs.mapper.material.StageDropStatisticsMapper;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StageDropStatisticsService {

    private final StageDropMapper stageDropMapper;

    private final StageDropStatisticsMapper stageDropStatisticsMapper;

    private final IdGenerator idGenerator;

    private final StageDropHourStatisticsMapper stageDropHourStatisticsMapper;

    public StageDropStatisticsService(StageDropMapper stageDropMapper,
            StageDropStatisticsMapper stageDropStatisticsMapper,
            StageDropHourStatisticsMapper stageDropHourStatisticsMapper) {
        this.stageDropMapper = stageDropMapper;
        this.stageDropStatisticsMapper = stageDropStatisticsMapper;
        this.idGenerator = new IdGenerator(6L);
        this.stageDropHourStatisticsMapper = stageDropHourStatisticsMapper;
    }

    public void day(Date startDate, Date endDate) {
        String itemUpdateJsonText = FileUtil.read(ConfigUtil.DataFilePath + "item_update.json");
        JsonNode itemUpdateTime = JsonMapper.parseJSONObject(itemUpdateJsonText);
        Map<String, Date> itemUpdateMap = new HashMap<>();

        for (JsonNode jsonNode : itemUpdateTime) {
            String itemId = jsonNode.get("itemId").asText();
            LocalDate localDate = LocalDate.parse(jsonNode.get("updateTime").asText());
            Date updateTime = Date.from(localDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
            itemUpdateMap.put(itemId, updateTime);
        }

        // 收集每个关卡在每个小时的执行次数
        // key: stageId + "_" + 小时时间戳, value: 该小时该关卡的最大times
        Map<String, StageDropTimesCountDTO> stageHourTimes = new HashMap<>();

        // 按 stageId + itemId 聚合数量和时间范围
        Map<String, StageDropQuantityCountDTO> quantityCollectMap = new HashMap<>();

        List<StageDropHourStatistics> list = stageDropHourStatisticsMapper.selectListByDate(
                TimeGranularity.HOUR.code(), startDate, endDate);

        for (StageDropHourStatistics item : list) {
            String stageId = item.getStageId();
            Long hourTimestamp = item.getStartTime().getTime();

            // 收集该时段该关卡的times（取最大值，同一关卡同一时段可能有多条材料记录）
            String stageTimeKey = stageId + "." + hourTimestamp;

            if (!stageHourTimes.containsKey(stageTimeKey)) {
                stageHourTimes.put(stageTimeKey, new StageDropTimesCountDTO(stageId, item.getItemId(), 0L));
            }
            StageDropTimesCountDTO stageDropTimesCountDTO = stageHourTimes.get(stageTimeKey);
            if (stageDropTimesCountDTO.getTimes() < item.getTimes()) {
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

    }

}
