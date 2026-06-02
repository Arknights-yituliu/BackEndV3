package com.lhs.service.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.IdGenerator;

import com.lhs.common.util.JsonMapper;
import com.lhs.mapper.material.StageDropMapper;
import com.lhs.mapper.material.StageDropStatisticsMapper;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class StageDropStatisticsService {

    private final StageDropMapper stageDropMapper;

    private final StageDropStatisticsMapper stageDropStatisticsMapper;

    private final IdGenerator idGenerator;

    public StageDropStatisticsService(StageDropMapper stageDropMapper,
            StageDropStatisticsMapper stageDropStatisticsMapper) {
        this.stageDropMapper = stageDropMapper;
        this.stageDropStatisticsMapper = stageDropStatisticsMapper;
        this.idGenerator = new IdGenerator(6L);

    }

    public void day() {
        String itemUpdateJsonText = FileUtil.read(ConfigUtil.DataFilePath + "item_update.json");
        JsonNode itemUpdateTime = JsonMapper.parseJSONObject(itemUpdateJsonText);
        Map<String, Date> itemUpdateMap = new HashMap<>();
        for (JsonNode jsonNode : itemUpdateTime) {
            String itemId = jsonNode.get("itemId").asText();
            LocalDate localDate = LocalDate.parse(jsonNode.get("updateTime").asText());
            Date updateTime = Date.from(localDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
            itemUpdateMap.put(itemId, updateTime);
        }


        

    }

}
