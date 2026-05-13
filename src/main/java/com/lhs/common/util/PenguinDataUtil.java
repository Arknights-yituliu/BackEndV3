package com.lhs.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.ConfigUtil;
import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;
import com.lhs.entity.dto.item.custom.OtherItemDropDTO;
import com.lhs.entity.dto.item.custom.StageDropAndInfoDTO;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.po.material.Stage;
import com.lhs.service.material.StageService;

public class PenguinDataUtil {

    private final StageService stageService;

    public PenguinDataUtil(StageService stageService) {
        this.stageService = stageService;
    }

    public Map<String, List<StageDropAndInfoDTO>> getStageDropCollect(ItemValueConfigDTO itemValueConfigDTO) {

        Map<String, List<StageDropAndInfoDTO>> stageDropCollect = new HashMap<>();
        String penguinMatrixText = FileUtil.read(ConfigUtil.Penguin + "penguin.json");
        String matrixText = JsonMapper.parseJSONObject(penguinMatrixText).get("matrix").toPrettyString();

        //
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText, new TypeReference<>() {
        });

        String ytlStageDataText = FileUtil.read(ConfigUtil.DataFilePath + "ytl_stage_info.json");

        Map<String, StageDropAndInfoDTO> ytlStageDataGroupByItemId = JsonMapper.parseJSONArray(ytlStageDataText,
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

            // 活动关卡21理智的，与一图流无限池数据合并
            if ("ACT".equals(stageType) && apCost == 21 && ytlStageDataGroupByItemId.containsKey(itemId)) {
                StageDropAndInfoDTO ytlData = ytlStageDataGroupByItemId.get(itemId);
                ytlData.setQuantity(ytlData.getQuantity() + quantity);
                ytlData.setTimes(ytlData.getTimes() + times);
            }

            StageDropAndInfoDTO mergeItem = new StageDropAndInfoDTO();
            mergeItem.setStageId(stageId);
            mergeItem.setItemId(itemId);
            mergeItem.setQuantity(quantity);
            mergeItem.setTimes(times);
            mergeItem.setStart(stageInfo.getStartTime() != null ? stageInfo.getStartTime().getTime() : null);
            mergeItem.setEnd(stageInfo.getEndTime() != null ? stageInfo.getEndTime().getTime() : null);
            mergeItem.setStageCode(stageCode);
            mergeItem.setApCost(apCost);
            mergeItem.setSpm(spm);
            mergeItem.setStageType(stageType);
            mergeItem.setZoneName(zoneName);
            mergeItem.setZoneId(zoneId);
            mergeItem.setUnlimitedItem(false);

            OtherItemDropDTO otherItemDropDTO = new OtherItemDropDTO();
            otherItemDropDTO.setItemId("4001");
            otherItemDropDTO.setQuantity(12L);
            otherItemDropDTO.setPrice(1.0);
            
            //关卡本身常规的龙门币掉落
            StageDropAndInfoDTO standardLMDDrop = createDropTemplate(stageInfo, otherItemDropDTO);

            // 将合并后的关卡掉落按关卡ID分组存入集合
            if (stageDropCollect.containsKey(stageId)) {
                stageDropCollect.get(stageId).add(mergeItem);
            } else {
                List<StageDropAndInfoDTO> dropList = new ArrayList<>();
                dropList.add(mergeItem);
                dropList.add(standardLMDDrop);
                stageDropCollect.put(stageId, dropList);
            }

        }

        // 遍历一图流模拟数据，将有效的模拟关卡存入关卡掉落集合
        for (Map.Entry<String, StageDropAndInfoDTO> entry : ytlStageDataGroupByItemId.entrySet()) {
            String itemId = entry.getKey();
            StageDropAndInfoDTO dropInfo = entry.getValue();
            if (dropInfo.getTimes() > 0) {
                OtherItemDropDTO lmdItem = new OtherItemDropDTO();
                lmdItem.setItemId("4001");
                lmdItem.setPrice(1.0);
                lmdItem.setQuantity(12L);

                StageDropAndInfoDTO lmdDrop = createDropTemplate(stageInfoMap.get(dropInfo.getStageId()), lmdItem);

                List<StageDropAndInfoDTO> dropList = new ArrayList<>();
                dropList.add(dropInfo);
                dropList.add(lmdDrop);
                stageDropCollect.put(dropInfo.getStageId(), dropList);
            }
        }

        return stageDropCollect;

    }

    /**
     * 根据关卡信息和无限池商品创建关卡掉落模板
     * @param stageInfo 关卡信息
     * @param shopRedemptionItem 无限池商品信息
     * @return 关卡掉落与信息DTO
     */
    public static StageDropAndInfoDTO createDropTemplate(Stage stageInfo, OtherItemDropDTO shopRedemptionItem) {
        StageDropAndInfoDTO dto = new StageDropAndInfoDTO();
        dto.setStageId(stageInfo.getStageId());
        dto.setItemId(shopRedemptionItem.getItemId());
        dto.setQuantity((long) (stageInfo.getApCost() / shopRedemptionItem.getPrice()
                * shopRedemptionItem.getQuantity() * 1000));
        dto.setTimes(1000L);
        dto.setStart(stageInfo.getStartTime() != null ? stageInfo.getStartTime().getTime() : null);
        dto.setEnd(stageInfo.getEndTime() != null ? stageInfo.getEndTime().getTime() : null);
        dto.setStageCode(stageInfo.getStageCode());
        dto.setApCost(stageInfo.getApCost());
        dto.setSpm(stageInfo.getSpm());
        dto.setUnlimitedItem(true);
        dto.setStageType(stageInfo.getStageType());
        dto.setZoneName(stageInfo.getZoneName());
        dto.setZoneId(stageInfo.getZoneId());
        return dto;
    }

}
