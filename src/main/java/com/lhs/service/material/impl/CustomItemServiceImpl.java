package com.lhs.service.material.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.PenguinMatrixCollect;
import com.lhs.entity.dto.item.*;
import com.lhs.entity.dto.item.CompositeTableDTO;
import com.lhs.entity.dto.material.*;
import com.lhs.entity.po.material.ItemInfo;
import com.lhs.entity.po.material.Stage;
import com.lhs.service.material.ItemService;
import com.lhs.service.material.StageCalService;
import com.lhs.service.material.StageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomItemServiceImpl {

    private final StageCalService stageCalService;

    private final ItemService itemService;

    private final StageService stageService;

    private final RedisTemplate<String, Object> redisTemplate;

    public CustomItemServiceImpl(StageCalService stageCalService, ItemService itemService, StageService stageService, RedisTemplate<String, Object> redisTemplate) {
        this.stageCalService = stageCalService;
        this.itemService = itemService;
        this.stageService = stageService;
        this.redisTemplate = redisTemplate;
    }

    private static final Double BASE_VALUE = 0.0036;

    private static final Map<Integer, Double> RECRUIT_RARITY = new HashMap<>();
    private static final Map<Integer, Map<String,Integer>> RECRUIT_TOKEN = new HashMap<>();
     static {
         RECRUIT_RARITY.put(3,0.7882);
         RECRUIT_RARITY.put(4,0.2027);
         RECRUIT_RARITY.put(5,0.0069);
         RECRUIT_RARITY.put(6,0.0022);

         RECRUIT_TOKEN.put(3, createToken(10, 0));
         RECRUIT_TOKEN.put(4, createToken(30, 1));
         RECRUIT_TOKEN.put(5, createToken(0, 5));
         RECRUIT_TOKEN.put(6, createToken(0, 10));
     }

    private static Map<String, Integer> createToken(int value4005, int value4004) {
        Map<String, Integer> map = new HashMap<>();
        map.put("4005", value4005);
        map.put("4004", value4004);
        return map;
    }


    public void getCustomItemValue(StageConfigDTO stageConfigDTO) {
        Map<String, Stage> stageInfoMap = stageService.getStageMapKeyIsStageId();
        Map<Integer, Double> expectedValueOfWorkshopByproductsMap = loadingExpectedValueOfWorkshopByproducts();
        Map<String, ItemValueCorrectionTerm> itemValueCorrectionTermMap = loadingItemValueCorrectionTerm();
        List<ItemInfo> itemInfoList = loadingItemInfo();
        Map<String, ItemSeriesInfoDTO> itemSeriesInfoMap = getItemSeriesInfoMap();

        Map<String, ItemInfo> itemInfoMap = new HashMap<>();

        for (ItemInfo itemInfo : itemInfoList) {
            itemInfoMap.put(itemInfo.getItemId(), itemInfo);
        }

        //根据关卡id分组的关卡集合
        Map<String, StageInfoAndDrop> stageInfoAndDropMap = PenguinMatrixCollect.getStageInfoAndDropMap(itemInfoMap, stageInfoMap, stageConfigDTO);

    }

    private void calculatedItemValue(StageConfigDTO stageConfigDTO,
                                     Map<String, ItemInfo> itemInfoMap,
                                     Map<String, ItemValueCorrectionTerm> itemValueCorrectionTermMap,
                                     Map<Integer, Double> expectedValueOfWorkshopByproductsMap) {

        List<CustomItemDTO> customItem = stageConfigDTO.getCustomItem();
        Double expCoefficient = stageConfigDTO.getExpCoefficient();
        Double lmdCoefficient = stageConfigDTO.getLmdCoefficient();
        Double workshopEliteMaterialByProductRate = stageConfigDTO.getWorkshopEliteMaterialByProductRate();
        Map<String, Double> customItemMap = new HashMap<>();
        for (CustomItemDTO item : customItem) {
            customItemMap.put(item.getItemId(), item.getItemValue());
        }

        for (ItemInfo itemInfo : itemInfoMap.values()) {
            String itemId = itemInfo.getItemId();
            //这里处理经验书和龙门币的价值，根据经验书和龙门币系数计算价值
            if ("2004".equals(itemId)) {
                itemInfo.setItemValue(BASE_VALUE * expCoefficient * 2000);
            }
            if ("2003".equals(itemId)) {
                itemInfo.setItemValue(BASE_VALUE * expCoefficient * 1000);
            }
            if ("2002".equals(itemId)) {
                itemInfo.setItemValue(BASE_VALUE * expCoefficient * 400);
            }
            if ("2001".equals(itemId)) {
                itemInfo.setItemValue(BASE_VALUE * expCoefficient * 200);
            }
            if ("4001".equals(itemId)) {
                itemInfo.setItemValue(BASE_VALUE * lmdCoefficient);
            }

            if (itemValueCorrectionTermMap.containsKey(itemId)) {
                // console.log(item.itemName, item.itemValue, '/', itemValueCorrectionTerm[itemId].correctionTerm)
                itemInfo.setItemValue(itemInfo.getItemValue() / itemValueCorrectionTermMap.get(itemId).getCorrectionTerm());
            }

            //如果物品被自定义价值了，将自定义价值强制写入
            if (customItemMap.containsKey(itemId)) {
                itemInfo.setItemValue(customItemMap.get(itemId));
            }

            //写入物品信息map
            itemInfoMap.put(itemId, itemInfo);
        }

        List<CompositeTableDTO> compositeTableList = getCompositeTable();
        for (CompositeTableDTO compositeTable : compositeTableList) {
            String outputItemId = compositeTable.getItemId();
            //如果这个物品被自定义了，不再通过合成路径得到价值
            if (customItemMap.containsKey(outputItemId)) {
                continue;
            }

            List<PathwayDTO> pathway = compositeTable.getPathway();
            Boolean resolve = compositeTable.getResolve();
            Integer rarity = compositeTable.getRarity();
            ItemInfo outputItemInfo = itemInfoMap.get(outputItemId);

            double newItemValue = 0.0;
            if (resolve) {
                //灰，绿色品质是向下拆解   灰，绿色物品 = （蓝物品价值 + 副产物 - 龙门币）/合成蓝物品的所需灰绿物品数量
                double expectedValueOfWorkshopByproducts = expectedValueOfWorkshopByproductsMap.get(rarity)
                        * workshopEliteMaterialByProductRate;
                PathwayDTO pathwayDTO = pathway.get(0);
                newItemValue = (outputItemInfo.getItemValue() + expectedValueOfWorkshopByproducts - BASE_VALUE * lmdCoefficient * rarity) / pathwayDTO.getCount();
            } else {
                //紫，金色品质是向上合成    紫，金色物品 =  合成所需蓝物品价值之和  + 龙门币 - 副产物
                double expectedValueOfWorkshopByproducts = expectedValueOfWorkshopByproductsMap.get(rarity - 1)
                        * workshopEliteMaterialByProductRate;
                double inputItemValue = 0.0;
                for (PathwayDTO pathwayDTO : pathway) {
                    ItemInfo inputItem = itemInfoMap.get(pathwayDTO.getItemId());
                    inputItemValue += inputItem.getItemValue() * pathwayDTO.getCount();
                }
                newItemValue = inputItemValue + BASE_VALUE * lmdCoefficient * (rarity - 1) - expectedValueOfWorkshopByproducts;
            }

            outputItemInfo.setItemValue(newItemValue);
        }
        getWorkShopProductValue(itemInfoMap,expectedValueOfWorkshopByproductsMap);
        calculateCommonItemValue(stageConfigDTO,itemInfoMap);
    }


    private static void getWorkShopProductValue(Map<String, ItemInfo> itemInfoMap,
                                         Map<Integer, Double> expectedValueOfWorkshopByproductsMap){
        Map<Integer, Double> collect = new HashMap<>();
        for(ItemInfo itemInfo:itemInfoMap.values()){
            if(itemInfo.getWeight()==0){
                continue;
            }
            Integer rarity = itemInfo.getRarity();
            if(!collect.containsKey(rarity)){
                collect.put(rarity,0.0);
            }

            collect.put(rarity,collect.get(rarity)+itemInfo.getItemValue()*itemInfo.getItemValue());
        }

        for(Integer rarity :collect.keySet()){
            expectedValueOfWorkshopByproductsMap.put(rarity,collect.get(rarity));
        }

    }

    private static void calculateCommonItemValue(StageConfigDTO stageConfigDTO,
                                                Map<String, ItemInfo> itemInfoMap) {

        Double workshopSkillSummaryByProductRate = stageConfigDTO.getWorkshopSkillSummaryByProductRate();
        Double workshopEliteMaterialByProductRate = stageConfigDTO.getWorkshopEliteMaterialByProductRate();

        // 至纯源石
        double itemValue4002 = 135;
        // 合成玉
        double itemValue4003 = itemValue4002 / 180;
        // 寻访凭证
        double itemValue7003 = 600 * itemValue4003;
        // 十连寻访凭证
        double itemValue7004 = 10 * itemValue7003;
        // 资质凭证价值根据经验法则定价为 0.8
        double itemValue4005 = 0.8;
        // 高级凭证
        double itemValue4004 = 38.0 / 258 * itemValue7003;
        // 中坚寻访凭证
        double itemValueClassicGacha = 216.0 / 38 * itemValue4004;
        // 十连中坚寻访凭证
        double itemValueClassicGacha10 = 10 * itemValueClassicGacha;

        // 家具零件
        double itemValue3401 = 0;
        // 加急许可
        double itemValue7002 = 0;

        // 龙门币
        double itemValueLMD = itemInfoMap.get("4001").getItemValue();
        // EXP
        double itemValueEXP = itemInfoMap.get("2001").getItemValue() / 200;
        // 无人机
        double itemValueBaseAp = itemValueEXP * 50 / 3;
        // 赤金
        double itemValue3003 = itemValueBaseAp * 24;

        // 采购凭证
        double itemValue4006 = 30 * (1 - itemValueLMD * 12) / 21;
        // 芯片助剂
        double itemValue32001 = itemValue4006 * 90;
        // 芯片
        double chip1Value = 18 * (1 - itemValueLMD * 12);
        // 芯片组
        double chip2Value = 36 * (1 - itemValueLMD * 12);
        // 双芯片
        double chip3Value = chip2Value * 2 + itemValue32001;
        // 模组数据块
        double itemValueModUnlockToken = 120 * itemValue4006;
        // 事相碎片
        double itemValueSTORYREVIEWCOIN = 20 * itemValue4006;

        // 工坊产品 t3（示例值）
        double t3workShopProductsValue = 100;
        // 因果
        double itemValueYinGuo = 10.0 / 9 * (1 - workshopEliteMaterialByProductRate) * t3workShopProductsValue / 36;
        // 碳素组
        double itemValue3114 = 240.0 / 19 * itemValue3401 + 4 * itemValueYinGuo - 4000.0 / 19 * itemValueLMD;
        double itemValue3113 = 11.0 / 30 * itemValue3114 + 6.0 / 5 * itemValueYinGuo;
        double itemValue3112 = 11.0 / 30 * itemValue3113 + 3.0 / 5 * itemValueYinGuo;

        // 技巧概要·卷3
        double denominator = 2 + (3.0 / 2 * (1 + workshopSkillSummaryByProductRate) / 3)
                + 1.5 * Math.pow((1 + workshopSkillSummaryByProductRate), 2) / 9;
        double itemValue3303 = 30 * (1 - itemValueLMD * 12) / denominator;

        // 技能概要·卷2
        double itemValue3302 = (1 + workshopSkillSummaryByProductRate) * itemValue3303 / 3;
        // 技能概要·卷1
        double itemValue3301 = (1 + workshopSkillSummaryByProductRate) * itemValue3302 / 3;

        // 招聘许可
        double itemValue7001 = 0;
        for (Integer rarity : RECRUIT_TOKEN.keySet()) {
            Map<String, Integer> token = RECRUIT_TOKEN.get(rarity);
            itemValue7001 += RECRUIT_RARITY.get(rarity) *
                    (token.get("4005") * itemValue4005 + token.get("4004") * itemValue4004);
        }

        // 合成玉（搓玉）
        double itemValue4003sp = (itemInfoMap.get("30012").getItemValue() * 2 +
                1600 * itemValueLMD + 40 * itemValueBaseAp) / 10;

        // 更新 itemInfoMap
        itemInfoMap.get("4002").setItemValue(itemValue4002);
        itemInfoMap.get("4003").setItemValue(itemValue4003);
        itemInfoMap.get("7003").setItemValue(itemValue7003);
        itemInfoMap.get("7004").setItemValue(itemValue7004);
        itemInfoMap.get("4005").setItemValue(itemValue4005);
        itemInfoMap.get("4004").setItemValue(itemValue4004);
        itemInfoMap.put("classic_gacha", new ItemInfo());
        itemInfoMap.get("classic_gacha").setItemValue(itemValueClassicGacha);
        itemInfoMap.put("classic_gacha_10", new ItemInfo());
        itemInfoMap.get("classic_gacha_10").setItemValue(itemValueClassicGacha10);
        itemInfoMap.get("3401").setItemValue(itemValue3401);
        itemInfoMap.put("base_ap", new ItemInfo());
        itemInfoMap.get("base_ap").setItemValue(itemValueBaseAp);
        itemInfoMap.get("3003").setItemValue(itemValue3003);
        itemInfoMap.get("4006").setItemValue(itemValue4006);
        itemInfoMap.get("32001").setItemValue(itemValue32001);
        itemInfoMap.put("mod_unlock_token", new ItemInfo());
        itemInfoMap.get("mod_unlock_token").setItemValue(itemValueModUnlockToken);
        itemInfoMap.put("STORY_REVIEW_COIN", new ItemInfo());
        itemInfoMap.get("STORY_REVIEW_COIN").setItemValue(itemValueSTORYREVIEWCOIN);
        itemInfoMap.get("3114").setItemValue(itemValue3114);
        itemInfoMap.get("3113").setItemValue(itemValue3113);
        itemInfoMap.get("3112").setItemValue(itemValue3112);
        itemInfoMap.get("3303").setItemValue(itemValue3303);
        itemInfoMap.get("3302").setItemValue(itemValue3302);
        itemInfoMap.get("3301").setItemValue(itemValue3301);
        itemInfoMap.get("7001").setItemValue(itemValue7001);
        itemInfoMap.get("4003sp").setItemValue(itemValue4003sp);

        // 处理芯片组
        String[][] chipIds = {
                {"3211", "3221", "3231", "3241", "3251", "3261", "3271", "3281"}, // chip1
                {"3212", "3222", "3232", "3242", "3252", "3262", "3272", "3282"}, // chip2
                {"3213", "3223", "3233", "3243", "3253", "3263", "3273", "3283"}  // chip3
        };

        for (int i = 0; i < chipIds.length; i++) {
            double value = (i == 0) ? chip1Value : (i == 1) ? chip2Value : chip3Value;
            for (String itemId : chipIds[i]) {
                if (!itemInfoMap.containsKey(itemId)) {
                    itemInfoMap.put(itemId, new ItemInfo());
                }
                itemInfoMap.get(itemId).setItemValue(value);
            }
        }
    }

    private void getItemValueCorrectionTerm(StageConfigDTO stageConfigDTO,
                                            Map<String, ItemInfo> itemInfoMap,
                                            Map<String, StageInfoAndDrop> stageInfoAndDropMap,
                                            Map<String, ItemSeriesInfoDTO> itemSeriesInfoMap,
                                            Map<String, ItemValueCorrectionTerm> itemValueCorrectionTermMap) {


        //每个物品系列的最高效率关卡
        HashMap<String, Double> itemSeriesMaxStageEfficiencyMap = new HashMap<>();

        //虚拟SideStory关卡效率集合
        HashMap<String, Double> activityAverageStageEfficiencyMap = new HashMap<>();


        for (String stageId : stageInfoAndDropMap.keySet()) {
            StageInfoAndDrop stageInfoAndDrop = stageInfoAndDropMap.get(stageId);
            List<PenguinMatrixDTO> dropList = stageInfoAndDrop.getDropList();
            Integer apCost = stageInfoAndDrop.getApCost();
            String stageCode = stageInfoAndDrop.getStageCode();
            String stageType = stageInfoAndDrop.getStageType();

            if ("ACT".equals(stageType) || "ACT_REP".equals(stageType)) {
                if (!stageConfigDTO.getUseActivityStage()) {
                    continue;
                }
            }

            if ("YTL_VIRTUAL".equals(stageType)) {
                if (!stageConfigDTO.getUseActivityAverageStage()) {
                    continue;
                }
            }

            double stageEfficiency = 0.0;
            double stageExpectedOutput = 0.0;
            String mainItemId = "0";
            double maxOutputValue = 0.0;

            for (PenguinMatrixDTO penguinMatrixDTO : dropList) {
                String itemId = penguinMatrixDTO.getItemId();
                Integer quantity = penguinMatrixDTO.getQuantity();
                Integer times = penguinMatrixDTO.getTimes();
                if (!itemInfoMap.containsKey(itemId)) {
                    continue;
                }
                ItemInfo itemInfo = itemInfoMap.get(itemId);
                double itemValue = itemInfo.getItemValue();
                String itemName = itemInfo.getItemName();
                //计算物品掉率
                double knockRating = (double) quantity / (double) times;
                //计算单项物品期望产出价值
                double expectedOutput = knockRating * itemValue;
                //比较单项物品最大产出，最大的为主产物
                if (expectedOutput > maxOutputValue) {
                    mainItemId = itemId;
                    maxOutputValue = expectedOutput;
                }
                //计算关卡期望产出总理智
                stageExpectedOutput += expectedOutput;
            }

            //计算关卡效率
            stageEfficiency = stageExpectedOutput / apCost;


            if (!itemSeriesInfoMap.containsKey(mainItemId)) {
                continue;
            }

            ItemSeriesInfoDTO itemSeriesInfoDTO = itemSeriesInfoMap.get(mainItemId);
            String seriesId = itemSeriesInfoDTO.getSeriesId();
            String seriesName = itemSeriesInfoDTO.getSeriesName();

            ItemValueCorrectionTerm itemValueCorrectionTerm = new ItemValueCorrectionTerm();
            itemValueCorrectionTerm.setSeriesName(seriesName);
            itemValueCorrectionTerm.setSeriesId(seriesId);
            itemValueCorrectionTerm.setCorrectionTerm(stageEfficiency);
            itemValueCorrectionTerm.setStageCode(stageCode);

            if (itemValueCorrectionTermMap.containsKey(seriesId)) {
                Double correctionTerm = itemValueCorrectionTermMap.get(seriesId).getCorrectionTerm();
                if (stageEfficiency > correctionTerm) {
                    itemValueCorrectionTermMap.put(seriesId, itemValueCorrectionTerm);
                }
            } else {
                itemValueCorrectionTermMap.put(seriesId, itemValueCorrectionTerm);
            }

        }

    }


    private Map<String, ItemSeriesInfoDTO> getItemSeriesInfoMap() {
        String read = FileUtil.read(ConfigUtil.DataFilePath + "item_series_info.json");
        JsonNode jsonNodeList = JsonMapper.parseJSONObject(read);
        HashMap<String, ItemSeriesInfoDTO> map = new HashMap<>();
        for (JsonNode item : jsonNodeList) {
            String seriesId = item.get("seriesId").asText();
            String seriesName = item.get("seriesName").asText();
            JsonNode itemSeries = item.get("itemSeries");
            for (JsonNode series : itemSeries) {
                ItemSeriesInfoDTO itemSeriesInfoDTO = new ItemSeriesInfoDTO();
                itemSeriesInfoDTO.setSeriesId(seriesId);
                itemSeriesInfoDTO.setSeriesName(seriesName);
                String itemId = series.get("itemId").asText();
                map.put(itemId, itemSeriesInfoDTO);
            }
        }

        return map;
    }

    private List<CompositeTableDTO> getCompositeTable() {
        String read = FileUtil.read(ConfigUtil.DataFilePath + "composite_table.v2.json");

        return JsonMapper.parseObject(read, new TypeReference<>() {
        });
    }

    private Map<Integer, Double> loadingExpectedValueOfWorkshopByproducts() {

        Map<Integer, Double> map = new HashMap<>();

        map.put(1, 1.962201);
        map.put(2, 5.853670784766422);
        map.put(3, 24.578868967312253);
        map.put(4, 79.8592285162492);
        return map;
    }

    private Map<String, ItemValueCorrectionTerm> loadingItemValueCorrectionTerm() {
        String read = FileUtil.read(ConfigUtil.DataFilePath + "item_value_correction_term.json");
        List<ItemValueCorrectionTerm> itemValueCorrectionTermList = JsonMapper.parseObject(read, new TypeReference<>() {
        });

        Map<String, ItemValueCorrectionTerm> map = new HashMap<>();
        for (ItemValueCorrectionTerm itemValueCorrectionTerm : itemValueCorrectionTermList) {
            map.put(itemValueCorrectionTerm.getSeriesId(), itemValueCorrectionTerm);
        }

        return map;
    }

    private List<ItemInfo> loadingItemInfo() {
        String read = FileUtil.read(ConfigUtil.DataFilePath + "item_info.json");
        return JsonMapper.parseObject(read, new TypeReference<>() {
        });
    }

    public void calculatedItemValue(StageConfigDTO stageConfigDTO) {


    }

}
