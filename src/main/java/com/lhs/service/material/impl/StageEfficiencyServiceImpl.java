package com.lhs.service.material.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.material.ItemInfoDTO;
import com.lhs.entity.dto.material.ItemValueConfigDTO;
import com.lhs.entity.dto.material.OtherItemDropDTO;
import com.lhs.entity.dto.material.StageDropAndInfoDTO;
import com.lhs.entity.po.material.Stage;
import com.lhs.entity.vo.material.HistoryActStageVO;
import com.lhs.entity.vo.material.RecommendedStageVO;
import com.lhs.entity.vo.material.StageDropDetailVO;
import com.lhs.entity.vo.material.StageEfficiencyResultVO;
import com.lhs.entity.vo.material.StageEfficiencyVO;
import com.lhs.service.material.CustomItemService;
import com.lhs.service.material.PenguinDataService;
import com.lhs.service.material.StageEfficiencyService;
import com.lhs.service.material.StageService;

/**
 * 关卡效率与推荐关卡服务实现
 * 逻辑参考前端 stageEfficiencyCal.js：先复用自定义价值接口得到物品价值，再据此计算各关卡效率并汇总推荐关卡
 */
@Service
public class StageEfficiencyServiceImpl implements StageEfficiencyService {

    /** 活动关卡类型 */
    private static final String STAGE_TYPE_ACT = "ACT";
    /** 复刻活动关卡类型 */
    private static final String STAGE_TYPE_ACT_REP = "ACT_REP";
    /** 活动平均掉率虚拟关卡类型 */
    private static final String STAGE_TYPE_YTL_VIRTUAL = "YTL_VIRTUAL";
    /** 龙门币物品ID */
    private static final String LMD_ITEM_ID = "4001";
    /** 默认关卡结束时间戳（2099 年，视为长期开放） */
    private static final long DEFAULT_END_TIME = 4073691312000L;
    /** 搓玉效率基准值：没有可比较关卡时用于兜底 */
    private static final double DEFAULT_MAX_ORUNDUM_PER_AP = 1.0898;

    /**
     * 商店无限池兑换物，用于计算活动关卡中额外兑换物的价值
     * 对应前端 unlimitedItemsInStore
     */
    private static final Map<String, ShopRedemptionItem> UNLIMITED_ITEMS_IN_STORE = new LinkedHashMap<>();

    static {
        UNLIMITED_ITEMS_IN_STORE.put("4001", new ShopRedemptionItem("4001", 20L, 1.0));
        UNLIMITED_ITEMS_IN_STORE.put("30073", new ShopRedemptionItem("30073", 1L, 25.0));
        UNLIMITED_ITEMS_IN_STORE.put("30083", new ShopRedemptionItem("30083", 1L, 30.0));
        UNLIMITED_ITEMS_IN_STORE.put("30093", new ShopRedemptionItem("30093", 1L, 35.0));
        UNLIMITED_ITEMS_IN_STORE.put("30103", new ShopRedemptionItem("30103", 1L, 40.0));
    }

    private final CustomItemService customItemService;
    private final PenguinDataService penguinDataService;
    private final StageService stageService;

    public StageEfficiencyServiceImpl(CustomItemService customItemService,
            PenguinDataService penguinDataService,
            StageService stageService) {
        this.customItemService = customItemService;
        this.penguinDataService = penguinDataService;
        this.stageService = stageService;
    }

    /**
     * 根据物品定价配置计算各关卡效率并汇总推荐关卡
     *
     * @param itemValueConfigDTO 物品定价配置（与自定义价值接口共用）
     * @return 推荐关卡汇总结果
     */
    @Override
    public StageEfficiencyResultVO getRecommendedStage(ItemValueConfigDTO itemValueConfigDTO) {
        // 复用自定义价值计算，得到带价值的物品信息（该方法会同时填充默认配置）
        List<ItemInfoDTO> itemInfoList = customItemService.getCustomItemList(itemValueConfigDTO);
        Map<String, ItemInfoDTO> itemInfoMap = new HashMap<>();
        for (ItemInfoDTO itemInfo : itemInfoList) {
            itemInfoMap.put(itemInfo.getItemId(), itemInfo);
        }

        // 关卡元数据与企鹅物流掉落数据
        Map<String, Stage> stageInfoMap = stageService.getStageInfoMap();
        Map<String, List<StageDropAndInfoDTO>> stageDropCollect = penguinDataService
                .getStageDropCollect(itemValueConfigDTO, stageInfoMap);

        // 物品系列信息：用于按主产物系列分组以及计算该系列各稀有度的理智转化率
        Map<String, ItemSeriesInfo> itemSeriesInfoByItemId = getItemSeriesInfoByItemId();

        // 商店无限池中当前价值最高的兑换物
        ShopRedemptionItem shopRedemptionItem = getShopRedemptionItem(itemInfoMap);

        boolean useActivityAverageStage = Boolean.TRUE.equals(itemValueConfigDTO.getUseActivityAverageStage());

        // 计算每个关卡的效率
        List<StageEfficiencyVO> stageResultList = calculateStageEfficiency(stageDropCollect, itemInfoMap,
                itemSeriesInfoByItemId, shopRedemptionItem, useActivityAverageStage);

        // 只保留仍开放的关卡用于当前推荐
        long now = System.currentTimeMillis();
        List<StageEfficiencyVO> openStageResult = new ArrayList<>();
        for (StageEfficiencyVO stage : stageResultList) {
            if (stage.getEnd() != null && stage.getEnd() > now) {
                openStageResult.add(stage);
            }
        }

        StageEfficiencyResultVO resultVO = new StageEfficiencyResultVO();
        // 先按材料系列分组，再计算搓玉效率，保证分组内的对象同样带有搓玉效率字段（与前端引用同一对象的逻辑一致）
        resultVO.setRecommendedStage(getRecommendedStageList(openStageResult));
        resultVO.setOrundumRecommendedStageVO(getOrundumRecommendedStage(openStageResult));
        resultVO.setHistoryActStage(getHistoryActStage(stageResultList));
        resultVO.setHistoryActOrundumStage(getHistoryActOrundumStage(stageResultList));
        return resultVO;
    }

    /**
     * 计算商店无限池中单位代币价值最高的兑换物
     *
     * @param itemInfoMap 物品信息表
     * @return 价值最高的兑换物，默认龙门币
     */
    private ShopRedemptionItem getShopRedemptionItem(Map<String, ItemInfoDTO> itemInfoMap) {
        ShopRedemptionItem best = new ShopRedemptionItem(LMD_ITEM_ID, 20L, 1.0);
        for (ShopRedemptionItem candidate : UNLIMITED_ITEMS_IN_STORE.values()) {
            ItemInfoDTO itemInfo = itemInfoMap.get(candidate.getItemId());
            if (itemInfo == null || itemInfo.getItemValue() == null) {
                continue;
            }
            // 效率 = (1 / 物品价格) * 物品数量 * 物品价值
            double apEfficiency = (1.0 / candidate.getPrice()) * candidate.getQuantity() * itemInfo.getItemValue();
            if (apEfficiency > best.getApEfficiency()) {
                ShopRedemptionItem currentBest = new ShopRedemptionItem(candidate.getItemId(),
                        candidate.getQuantity(), candidate.getPrice());
                currentBest.setApEfficiency(apEfficiency);
                best = currentBest;
            }
        }
        return best;
    }

    /**
     * 计算所有关卡的效率
     *
     * @param stageDropCollect         关卡掉落集合
     * @param itemInfoMap              物品信息表
     * @param itemSeriesInfoByItemId   物品系列信息
     * @param shopRedemptionItem       商店无限池最优兑换物
     * @param useActivityAverageStage  是否使用活动平均掉率关卡
     * @return 关卡效率结果集合
     */
    private List<StageEfficiencyVO> calculateStageEfficiency(Map<String, List<StageDropAndInfoDTO>> stageDropCollect,
            Map<String, ItemInfoDTO> itemInfoMap,
            Map<String, ItemSeriesInfo> itemSeriesInfoByItemId,
            ShopRedemptionItem shopRedemptionItem,
            boolean useActivityAverageStage) {

        List<StageEfficiencyVO> stageResultList = new ArrayList<>();

        for (Map.Entry<String, List<StageDropAndInfoDTO>> entry : stageDropCollect.entrySet()) {
            List<StageDropAndInfoDTO> sourceDropList = entry.getValue();
            if (sourceDropList == null || sourceDropList.isEmpty()) {
                continue;
            }

            // 复制一份，避免向原始掉落集合追加虚拟掉落物
            List<StageDropAndInfoDTO> dropList = new ArrayList<>(sourceDropList);
            StageDropAndInfoDTO firstDrop = dropList.get(0);
            String stageType = firstDrop.getStageType();

            // 不使用活动平均掉率时，跳过临时商店虚拟关卡
            if (!useActivityAverageStage && STAGE_TYPE_YTL_VIRTUAL.equals(stageType)) {
                continue;
            }

            // 活动关卡追加商店无限池兑换物的虚拟掉落
            if (STAGE_TYPE_ACT.equals(stageType) || STAGE_TYPE_ACT_REP.equals(stageType)
                    || STAGE_TYPE_YTL_VIRTUAL.equals(stageType)) {
                OtherItemDropDTO shopItem = new OtherItemDropDTO();
                shopItem.setItemId(shopRedemptionItem.getItemId());
                shopItem.setQuantity(shopRedemptionItem.getQuantity());
                shopItem.setPrice(shopRedemptionItem.getPrice());
                dropList.add(PenguinDataServiceImpl.createDropTemplate(firstDrop, shopItem));
            }

            Integer apCostValue = firstDrop.getApCost();
            if (apCostValue == null || apCostValue <= 0) {
                continue;
            }
            double apCost = apCostValue;

            // 计算掉落明细
            List<StageDropDetailVO> stageDropDetailList = new ArrayList<>();
            double stageExpectedOutput = 0.0;
            for (StageDropAndInfoDTO drop : dropList) {
                ItemInfoDTO itemInfo = itemInfoMap.get(drop.getItemId());
                if (itemInfo == null || itemInfo.getItemValue() == null) {
                    continue;
                }
                Long quantityObj = drop.getQuantity();
                Long timesObj = drop.getTimes();
                if (quantityObj == null || timesObj == null || timesObj <= 0) {
                    continue;
                }
                long quantity = quantityObj;
                long times = timesObj;
                // 材料掉率
                double knockRating = (double) quantity / times;
                // 单项物品期望产出价值
                double expectedOutput = knockRating * itemInfo.getItemValue();
                stageExpectedOutput += expectedOutput;

                StageDropDetailVO detail = new StageDropDetailVO();
                detail.setItemName(itemInfo.getItemName());
                detail.setItemId(drop.getItemId());
                detail.setItemValue(itemInfo.getItemValue());
                detail.setExpectedOutput(expectedOutput);
                detail.setKnockRating(knockRating);
                detail.setIsUnlimited(Boolean.TRUE.equals(drop.getUnlimitedItem()));
                detail.setRarity(itemInfo.getRarity());
                detail.setQuantity(quantity);
                detail.setTimes(times);
                detail.setSampleSize(times);
                detail.setEnd(firstDrop.getEnd());
                stageDropDetailList.add(detail);
            }

            // 按期望产出价值降序
            stageDropDetailList.sort((a, b) -> Double.compare(b.getExpectedOutput(), a.getExpectedOutput()));

            // 若价值最高的是无限兑换物虚拟掉落，则与第二名交换，保证主产物为真实掉落物
            if (stageDropDetailList.size() >= 2 && Boolean.TRUE.equals(stageDropDetailList.get(0).getIsUnlimited())) {
                StageDropDetailVO firstDetail = stageDropDetailList.get(0);
                stageDropDetailList.set(0, stageDropDetailList.get(1));
                stageDropDetailList.set(1, firstDetail);
            }

            ItemSeriesInfo mainSeriesInfo = null;
            double leT4Value = 0.0;
            double leT3Value = 0.0;
            double leT2Value = 0.0;
            double mainApExpect = 0.0;
            double mainKnockRating = 0.0;
            String secondaryItemId = "";
            long mainSampleSize = 0;
            double orundumPerAp = 0.0;
            double lmdCostPerAp = 0.0;
            int itemRarity = 0;
            String mainItemId = "0";
            String mainItemName = "0";
            Long endTimeStamp = DEFAULT_END_TIME;

            for (int i = 0; i < stageDropDetailList.size(); i++) {
                StageDropDetailVO element = stageDropDetailList.get(i);

                if (i == 0) {
                    mainSeriesInfo = itemSeriesInfoByItemId.get(element.getItemId());
                    if (firstDrop.getEnd() != null) {
                        endTimeStamp = firstDrop.getEnd();
                    }
                    mainItemId = element.getItemId();
                    mainItemName = element.getItemName();
                    itemRarity = element.getRarity() == null ? 0 : element.getRarity();
                    mainApExpect = apCost / element.getKnockRating();
                    mainKnockRating = element.getKnockRating();
                    mainSampleSize = element.getSampleSize() == null ? 0 : element.getSampleSize();
                }

                // 主产物不是精英材料则跳过该关卡
                if (mainSeriesInfo == null) {
                    break;
                }

                // 计算四种可搓玉材料的合成玉产出与龙门币消耗
                String detailItemId = element.getItemId();
                double detailKnockRating = element.getKnockRating();
                if ("30011".equals(detailItemId)) {
                    orundumPerAp += detailKnockRating * 5 / 3;
                    lmdCostPerAp += detailKnockRating * 800 / 3;
                } else if ("30012".equals(detailItemId)) {
                    orundumPerAp += detailKnockRating * 5;
                    lmdCostPerAp += detailKnockRating * 800;
                } else if ("30061".equals(detailItemId)) {
                    orundumPerAp += detailKnockRating * 10 / 3;
                    lmdCostPerAp += detailKnockRating * 1000 / 3;
                } else if ("30062".equals(detailItemId)) {
                    orundumPerAp += detailKnockRating * 10;
                    lmdCostPerAp += detailKnockRating * 1000;
                }

                // 产出排名第二且理智转化率大于 0.1 的材料记为副产物
                if (i == 1 && element.getExpectedOutput() / apCost > 0.1) {
                    secondaryItemId = detailItemId;
                }

                // 统计与主产物同系列材料的 T2、T3、T4 效率
                ItemSeriesInfo seriesInfo = itemSeriesInfoByItemId.get(detailItemId);
                if (seriesInfo != null && seriesInfo.getSeriesId().equals(mainSeriesInfo.getSeriesId())) {
                    int rarity = element.getRarity() == null ? 0 : element.getRarity();
                    if (rarity == 1 || rarity == 2) {
                        leT2Value += element.getExpectedOutput();
                    } else if (rarity == 3) {
                        leT3Value += element.getExpectedOutput();
                    } else if (rarity == 4) {
                        leT4Value += element.getExpectedOutput();
                    }
                }
            }

            // 主产物不属于任何精英材料系列，丢弃该关卡结果
            if (mainSeriesInfo == null) {
                continue;
            }

            orundumPerAp = orundumPerAp / apCost;
            double stageEfficiency = stageExpectedOutput / apCost;
            double leT4Efficiency = (leT2Value + leT3Value + leT4Value) / apCost;
            double leT3Efficiency = (leT2Value + leT3Value) / apCost;
            double leT2Efficiency = leT2Value / apCost;

            StageEfficiencyVO stageResult = new StageEfficiencyVO();
            stageResult.setStageCode(firstDrop.getStageCode());
            stageResult.setStageId(entry.getKey());
            stageResult.setStageType(stageType);
            stageResult.setZoneName(firstDrop.getZoneName());
            stageResult.setItemName(mainItemName);
            stageResult.setItemId(mainItemId);
            stageResult.setItemSeriesId(mainSeriesInfo.getSeriesId());
            stageResult.setItemRarity(itemRarity);
            stageResult.setSecondaryItemId(secondaryItemId);
            stageResult.setApExpect(mainApExpect);
            stageResult.setKnockRating(mainKnockRating);
            stageResult.setSampleSize(mainSampleSize);
            stageResult.setStageEfficiency(stageEfficiency);
            stageResult.setLeT2Efficiency(leT2Efficiency);
            stageResult.setLeT3Efficiency(leT3Efficiency);
            stageResult.setLeT4Efficiency(leT4Efficiency);
            stageResult.setOrundumPerAp(orundumPerAp);
            // 未产出搓玉材料时，龙门币消耗无意义，置空（与前端 JSON 序列化后的 null 一致）
            stageResult.setLmdCost(orundumPerAp == 0 ? null
                    : (lmdCostPerAp * 600) / apCost / orundumPerAp / 10000);
            stageResult.setEnd(endTimeStamp);
            stageResult.setSpm(firstDrop.getSpm());
            stageResult.setApCost(apCostValue);
            stageResult.setStageExpectedOutput(stageExpectedOutput);
            stageResult.setDropDetail(stageDropDetailList);
            stageResultList.add(stageResult);
        }

        return stageResultList;
    }

    /**
     * 按材料系列聚合推荐关卡
     *
     * @param stageResultList 开放中的关卡结果
     * @return 各材料系列的推荐关卡
     */
    private List<RecommendedStageVO> getRecommendedStageList(List<StageEfficiencyVO> stageResultList) {
        Map<String, List<StageEfficiencyVO>> recommendedStageCollect = new LinkedHashMap<>();
        for (StageEfficiencyVO stage : stageResultList) {
            // 理智转化率低于 0.5 的关卡不推荐
            if (stage.getStageEfficiency() == null || stage.getStageEfficiency() <= 0.5) {
                continue;
            }
            recommendedStageCollect.computeIfAbsent(stage.getItemSeriesId(), key -> new ArrayList<>()).add(stage);
        }

        List<RecommendedStageVO> recommendedStage = new ArrayList<>(recommendedStageCollect.size());
        for (Map.Entry<String, List<StageEfficiencyVO>> entry : recommendedStageCollect.entrySet()) {
            List<StageEfficiencyVO> stageList = entry.getValue();
            stageList.sort((a, b) -> Double.compare(b.getStageEfficiency(), a.getStageEfficiency()));
            RecommendedStageVO recommendedStageVO = new RecommendedStageVO();
            recommendedStageVO.setItemSeriesId(entry.getKey());
            recommendedStageVO.setStageResultList(stageList);
            recommendedStage.add(recommendedStageVO);
        }
        return recommendedStage;
    }

    /**
     * 计算搓玉推荐关卡
     *
     * @param stageResultList 关卡结果集合
     * @return 搓玉推荐关卡，按每理智合成玉产出降序，并写入相对效率
     */
    private List<StageEfficiencyVO> getOrundumRecommendedStage(List<StageEfficiencyVO> stageResultList) {
        List<StageEfficiencyVO> orundumStageList = new ArrayList<>();
        for (StageEfficiencyVO stage : stageResultList) {
            // 每理智合成玉产出大于 0.2 才纳入搓玉推荐
            if (stage.getOrundumPerAp() != null && stage.getOrundumPerAp() > 0.2) {
                orundumStageList.add(stage);
            }
        }
        orundumStageList.sort((a, b) -> Double.compare(b.getOrundumPerAp(), a.getOrundumPerAp()));

        double maxOrundumPerAp = orundumStageList.size() > 1
                ? orundumStageList.get(0).getOrundumPerAp()
                : DEFAULT_MAX_ORUNDUM_PER_AP;
        if (maxOrundumPerAp == 0) {
            maxOrundumPerAp = DEFAULT_MAX_ORUNDUM_PER_AP;
        }
        for (StageEfficiencyVO stage : orundumStageList) {
            stage.setOrundumPerApEfficiency(stage.getOrundumPerAp() / maxOrundumPerAp);
        }
        return orundumStageList;
    }

    /**
     * 计算历史活动关卡（仅 T3 主产物的活动关卡，按活动聚合）
     *
     * @param stageResultList 全部关卡结果
     * @return 历史活动关卡集合
     */
    private List<HistoryActStageVO> getHistoryActStage(List<StageEfficiencyVO> stageResultList) {
        Map<String, List<StageEfficiencyVO>> historyActStageCollect = new LinkedHashMap<>();
        for (StageEfficiencyVO stage : stageResultList) {
            boolean isActStage = STAGE_TYPE_ACT.equals(stage.getStageType())
                    || STAGE_TYPE_ACT_REP.equals(stage.getStageType());
            if (!isActStage || !Integer.valueOf(3).equals(stage.getItemRarity())) {
                continue;
            }
            historyActStageCollect.computeIfAbsent(stage.getZoneName(), key -> new ArrayList<>()).add(stage);
        }

        List<HistoryActStageVO> historyActStageList = new ArrayList<>();
        for (List<StageEfficiencyVO> stageList : historyActStageCollect.values()) {
            stageList.sort((a, b) -> Double.compare(b.getStageEfficiency(), a.getStageEfficiency()));
            historyActStageList.add(buildHistoryActStageVO(stageList));
        }
        historyActStageList.sort((a, b) -> Long.compare(b.getEndTime(), a.getEndTime()));
        return historyActStageList;
    }

    /**
     * 计算历史活动搓玉关卡（按活动聚合）
     *
     * @param stageResultList 全部关卡结果
     * @return 历史活动搓玉关卡集合
     */
    private List<HistoryActStageVO> getHistoryActOrundumStage(List<StageEfficiencyVO> stageResultList) {
        // 复制对象，避免覆盖开放推荐列表中同关卡已计算的搓玉效率
        List<StageEfficiencyVO> actStageCopies = new ArrayList<>();
        for (StageEfficiencyVO stage : stageResultList) {
            if (STAGE_TYPE_ACT.equals(stage.getStageType()) || STAGE_TYPE_ACT_REP.equals(stage.getStageType())) {
                actStageCopies.add(copyStageEfficiencyVO(stage));
            }
        }

        List<StageEfficiencyVO> orundumStageResultList = getOrundumRecommendedStage(actStageCopies);

        Map<String, List<StageEfficiencyVO>> historyActOrundumStageCollect = new LinkedHashMap<>();
        for (StageEfficiencyVO stage : orundumStageResultList) {
            String key = stage.getZoneName() + "_" + stage.getEnd();
            historyActOrundumStageCollect.computeIfAbsent(key, k -> new ArrayList<>()).add(stage);
        }

        List<HistoryActStageVO> historyActOrundumStageList = new ArrayList<>();
        for (List<StageEfficiencyVO> stageList : historyActOrundumStageCollect.values()) {
            stageList.sort((a, b) -> Double.compare(b.getOrundumPerAp(), a.getOrundumPerAp()));
            historyActOrundumStageList.add(buildHistoryActStageVO(stageList));
        }
        historyActOrundumStageList.sort((a, b) -> Long.compare(b.getEndTime(), a.getEndTime()));
        return historyActOrundumStageList;
    }

    /**
     * 由同一活动的关卡列表构建历史活动关卡 VO
     *
     * @param stageList 同一活动下的关卡列表（已排序）
     * @return 历史活动关卡 VO
     */
    private HistoryActStageVO buildHistoryActStageVO(List<StageEfficiencyVO> stageList) {
        StageEfficiencyVO firstStage = stageList.get(0);
        HistoryActStageVO historyActStageVO = new HistoryActStageVO();
        historyActStageVO.setZoneName(firstStage.getZoneName());
        historyActStageVO.setStageType(firstStage.getStageType());
        historyActStageVO.setActStageList(stageList);
        historyActStageVO.setEndTime(firstStage.getEnd());
        return historyActStageVO;
    }

    /**
     * 浅拷贝关卡结果（对应前端展开运算符复制对象的语义）
     *
     * @param source 源对象
     * @return 拷贝对象
     */
    private StageEfficiencyVO copyStageEfficiencyVO(StageEfficiencyVO source) {
        StageEfficiencyVO copy = new StageEfficiencyVO();
        BeanUtils.copyProperties(source, copy);
        return copy;
    }

    /**
     * 读取物品系列信息，构建 物品ID -> 系列信息 的映射
     *
     * @return 物品系列信息映射
     */
    private Map<String, ItemSeriesInfo> getItemSeriesInfoByItemId() {
        Map<String, ItemSeriesInfo> itemSeriesInfoByItemId = new HashMap<>();
        JsonNode itemSeriesInfoNode = JsonMapper
                .parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "item_series_info.json"));
        if (itemSeriesInfoNode == null) {
            return itemSeriesInfoByItemId;
        }
        for (JsonNode seriesInfoNode : itemSeriesInfoNode) {
            String seriesId = seriesInfoNode.get("seriesId").asText();
            JsonNode itemSeries = seriesInfoNode.get("itemSeries");
            if (itemSeries == null) {
                continue;
            }
            for (JsonNode item : itemSeries) {
                itemSeriesInfoByItemId.put(item.get("itemId").asText(), new ItemSeriesInfo(seriesId));
            }
        }
        return itemSeriesInfoByItemId;
    }

    /**
     * 商店无限池兑换物
     */
    private static class ShopRedemptionItem {
        /** 物品ID */
        private final String itemId;
        /** 兑换数量 */
        private final Long quantity;
        /** 兑换价格（代币） */
        private final Double price;
        /** 单位代币的效率 */
        private Double apEfficiency = 0.0;

        private ShopRedemptionItem(String itemId, Long quantity, Double price) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.price = price;
        }

        public String getItemId() {
            return itemId;
        }

        public Long getQuantity() {
            return quantity;
        }

        public Double getPrice() {
            return price;
        }

        public Double getApEfficiency() {
            return apEfficiency;
        }

        public void setApEfficiency(Double apEfficiency) {
            this.apEfficiency = apEfficiency;
        }
    }

    /**
     * 物品系列信息
     */
    private static class ItemSeriesInfo {
        /** 材料系列ID */
        private final String seriesId;

        private ItemSeriesInfo(String seriesId) {
            this.seriesId = seriesId;
        }

        public String getSeriesId() {
            return seriesId;
        }
    }
}
