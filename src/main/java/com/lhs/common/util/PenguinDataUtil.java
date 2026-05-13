package com.lhs.common.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.ConfigUtil;
import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;
import com.lhs.entity.dto.item.custom.StageInfoAndDropDTO;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.po.material.Stage;
import com.lhs.service.material.StageService;

public class PenguinDataUtil {

    private final StageService stageService;

    public PenguinDataUtil(StageService stageService) {
        this.stageService = stageService;
    }

    public Map<String, List<StageInfoAndDropDTO>> getStageDropCollect(ItemValueConfigDTO itemValueConfigDTO) {

        Map<String, List<StageInfoAndDropDTO>> stageDropCollect = new HashMap<>();
        String penguinMatrixText = FileUtil.read(ConfigUtil.Penguin + "penguin.json");
        String matrixText = JsonMapper.parseJSONObject(penguinMatrixText).get("matrix").toPrettyString();

        //
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText, new TypeReference<>() {
        });

        String ytlStageDataText = FileUtil.read(ConfigUtil.DataFilePath + "ytl_stage_info.json");

        Map<String, StageInfoAndDropDTO> ytlStageDataGroupByItemId = JsonMapper.parseJSONArray(ytlStageDataText,
                new TypeReference<>() {
                });

        Map<String, Stage> stageInfoMap = stageService.getStageInfoMap();

        // 将磨难关卡分离出来，方便后面与标准关卡的掉率进行合并
        List<PenguinMatrixDTO> toughStage = penguinMatrixDTOList.stream()
                .filter(e -> e.getStageId().contains("tough"))
                .collect(Collectors.toList());

        Map<String, PenguinMatrixDTO> toughStageMap = new HashMap<>();
        for (PenguinMatrixDTO item : toughStage) {
            String key = item.getStageId().replace("tough", "main") + "-" + item.getItemId();
            toughStageMap.put(key, item);
        }

        // 关卡黑名单
        Map<String, Integer> stageBlacklistMap = new HashMap<>();
        
        if (itemValueConfigDTO.getStageBlacklist() != null) {
            for (String item : itemValueConfigDTO.getStageBlacklist()) {
                stageBlacklistMap.put(item, 1);
            }
        }

        int sampleSize = 300;
        if (itemValueConfigDTO.getSampleSize() != null) {
            sampleSize = itemValueConfigDTO.getSampleSize();
        }

        // 遍历企鹅物流矩阵数据，过滤并合并磨难关卡数据
        for (PenguinMatrixDTO item : penguinMatrixDTOList) {
            String stageId = item.getStageId();
            String itemId = item.getItemId();
            Long quantity = item.getQuantity();
            Long times = item.getTimes();
            Long start = item.getStart();
            Long end = item.getEnd();

            // 跳过黑名单关卡
            if (stageBlacklistMap.get(stageId) != null) {
                continue;
            }

            // 跳过14章主线且有结束时间的关卡
            if (stageId.contains("main_14") && end != null) {
                continue;
            }

            // 跳过磨难关卡（后续通过toughStageMap合并）
            if (stageId.contains("tough")) {
                continue;
            }

            // 样本量不足则跳过
            if (times < sampleSize) {
                continue;
            }

            // 关卡不在关卡表中则跳过
            if (!stageInfoMap.containsKey(stageId)) {
                continue;
            }

            Stage stageInfo = stageInfoMap.get(stageId);

            // 查找对应的磨难关卡数据并合并数量和次数
            String toughKey = item.getStageId() + "-" + item.getItemId();
            if (toughStageMap.get(toughKey) != null) {
                PenguinMatrixDTO toughData = toughStageMap.get(toughKey);
                quantity = toughData.getQuantity() + quantity;
                times = toughData.getTimes() + times;
            }

            String stageCode = stageInfo.getStageCode();
            Integer apCost = stageInfo.getApCost();
            Double spm = stageInfo.getSpm();
            String stageType = stageInfo.getStageType();
            String zoneName = stageInfo.getZoneName();
            String zoneId = stageInfo.getZoneId();
        }

        return null;

    }
}
