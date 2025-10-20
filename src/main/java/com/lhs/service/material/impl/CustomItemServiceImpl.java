package com.lhs.service.material.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.enums.StageType;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.item.custom.*;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.po.material.Stage;
import com.lhs.service.material.CustomItemService;
import com.lhs.service.material.StageService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomItemServiceImpl implements CustomItemService {

    private final StageService stageService;

    private static final Integer BASE_LMD_VALUE = 36 / 10000;
    private static final Integer BASE_EXP_VALUE = 36 / 10000;

    public CustomItemServiceImpl(StageService stageService) {
        this.stageService = stageService;
    }

    @Override
    public void customItemValueCalculation() {

    }


    @Override
    public void getCustomItemList(ItemValueConfigDTO itemValueConfigDTO, Integer maxIteration) {

        Double tolerance = 0.000001;

        checkItemValueConfig(itemValueConfigDTO);

        JsonNode recruitmentTableJson = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "recruitment_table.json"));

        //招募许可定价方案
        JsonNode recruitmentPermitPricing = recruitmentTableJson.get("recruitmentPermitPricing");
        //公开招募干员概率
        JsonNode operatorRecruitmentRates = recruitmentTableJson.get("operatorRecruitmentRates");


        JsonNode compositeTable = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "composite_table.v2.json"));


        String itemInfoTableText = FileUtil.read(ConfigUtil.DataFilePath + "item_info_table.json");
        Map<String, ItemInfoDTO> itemInfoMap = JsonMapper.parseJSONArray(itemInfoTableText, new TypeReference<>() {
        });

        Map<String, Double> itemValueMap = new HashMap<>();
        for (ItemInfoDTO itemInfoDTO : itemInfoMap.values()) {
            String itemId = itemInfoDTO.getItemId();
            if ("精英材料".equals(itemInfoDTO.getType())) {
                itemValueMap.put(itemId, itemInfoDTO.getRarity() * 3.0);
            } else {
                itemValueMap.put(itemId, 0.0);
            }
        }

        itemValueMap.put("causality", 0.0);
        itemValueMap.put("AP_GAMEPLAY", 1.0);
        itemValueMap.put("EXP", 0.0);

        Map<Integer, Double> workshopByproductExpectedValue = Map.of(
                1, 3.0,
                2, 9.0,
                3, 27.0,
                4, 81.0
        );

        List<CustomItemDTO> customItem = itemValueConfigDTO.getCustomItem();
        Map<String, Double> customItemValueMap = customItem.stream().collect(Collectors.toMap(CustomItemDTO::getItemId, CustomItemDTO::getItemValue));

        //将精英材料根据品质进行分类，方便后面计算每级品质精英材料的加工站期望产出
        Map<Integer, Map<String, Double>> workshopByproductWeightMap = new HashMap<>();
        //循环所有精英材料
        for (ItemInfoDTO itemInfoDTO : itemInfoMap.values()) {
            if ("精英材料".equals(itemInfoDTO.getType())) {
                //权重值
                double weight = itemInfoDTO.getWeight();
                //稀有度
                int rarity = itemInfoDTO.getRarity();
                //物品ID
                String itemId = itemInfoDTO.getItemId();
                //将物品权重根据稀有度进行分类存入map
                workshopByproductWeightMap.computeIfAbsent(rarity, k -> new HashMap<>()).put(itemId, weight);
            }
        }

        Map<String, List<StageInfoAndDropDTO>> stageInfoAndDropCollect = getStageInfoAndDropCollect(itemValueConfigDTO);


        for (int i = 0; i < tolerance; i++) {
            calculateCommonItemValue(itemValueMap, itemValueConfigDTO, recruitmentPermitPricing, operatorRecruitmentRates);
            calculateEliteMaterialValueFromT3(itemValueMap, itemValueConfigDTO, workshopByproductExpectedValue, compositeTable,customItemValueMap);
        }

    }


    private void calculateCommonItemValue(Map<String, Double> itemValueMap, ItemValueConfigDTO itemValueConfigDTO, JsonNode recruitmentPermitPricing, JsonNode operatorRecruitmentRates) {
        // 因果价值
        double causalityValue = itemValueMap.get("causality");

        // 龙门币价值 = (36 ÷ 10000) × 龙门币价值系数
        double itemValue4001 = itemValueConfigDTO.getLmdCoefficient() * BASE_LMD_VALUE;

        // EXP 价值 = (36 ÷ 10000) × EXP 价值系数
        double itemValueEXP = itemValueConfigDTO.getExpCoefficient() * BASE_EXP_VALUE;

        double itemValue2001 = itemValueEXP * 200;
        double itemValue2002 = itemValueEXP * 400;
        double itemValue2003 = itemValueEXP * 1000;
        double itemValue2004 = itemValueEXP * 2000;

        // 无人机价值 = EXP 价值 × 无人机生产 EXP 数量
        double itemValueBaseAp = itemValueEXP * 50 / 3;

        // 赤金价值 = 无人机价值 ÷ 无人机生产赤金数量
        double itemValue3003 = itemValueBaseAp * 24;

        // 合成玉价值
        double itemValue4003;
        switch (itemValueConfigDTO.getOrundumPricingStrategy()) {
            case "ORUNDUM_PRICING_ORININUM_FARMING_ORIROCK_CUBE":  // 搓玉途径定价（用固源岩搓玉）
                itemValue4003 = (itemValueMap.get("30012") * 2 + 1600 * itemValue4001 + 40 * itemValueBaseAp) / 10;
                break;
            case "ORUNDUM_PRICING_ORININUM_FARMING_DEVICE":  // 搓玉途径定价（用装置搓玉）
                itemValue4003 = (itemValueMap.get("30062") + 1000 * itemValue4001 + 40 * itemValueBaseAp) / 10;
                break;
            default:
                itemValue4003 = itemValueConfigDTO.getOrundumValue();
        }

        // 至纯源石价值 */
        double itemValue4002;
        if (itemValueConfigDTO.getOriginitePrimeCoefficient() == Double.POSITIVE_INFINITY) {
            itemValue4002 = Double.POSITIVE_INFINITY;
        } else {
            itemValue4002 = itemValueConfigDTO.getOriginitePrimeCoefficient() * itemValue4003;
        }

        // 寻访凭证价值 = 600 × 合成玉价值 */
        double itemValue7003 = 600 * itemValue4003;
        // 十连寻访凭证价值 = 10 × 寻访凭证价值 */
        double itemValue7004 = 10 * itemValue7003;
        // 资质凭证价值根据经验法则定价为 0.8 */
        double itemValue4005 = 0.8;
        // 高级凭证价值 = 38 ÷ 258 × 寻访凭证价值 */
        double itemValue4004 = 38 / 258.0 * itemValue7003;

        // 中坚寻访凭证价值 */
        double itemValueClassicGacha;
        if (itemValueConfigDTO.getKernelHeadhuntingPermitCoefficient() == 0) {
            itemValueClassicGacha = 0;
        } else {
            itemValueClassicGacha = itemValueConfigDTO.getKernelHeadhuntingPermitCoefficient() * itemValue7003;
        }
        // 十连中坚寻访凭证价值 = 10 × 中坚寻访凭证价值 */
        double itemValueClassicGacha10 = 10 * itemValueClassicGacha;

        // 招聘许可价值 */
        double itemValue7001 = 0;
        if (itemValue4004 == Double.POSITIVE_INFINITY) {
            itemValue7001 = Double.POSITIVE_INFINITY;
        } else {
            JsonNode recruitmentPermitPricingStrategy = recruitmentPermitPricing.get(itemValueConfigDTO.getRecruitmentPermitPricingStrategy());
            Iterator<Map.Entry<String, JsonNode>> fields = recruitmentPermitPricingStrategy.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();
                JsonNode strategy = next.getValue();
                String rarity = next.getKey();
                itemValue7001 += operatorRecruitmentRates.get(rarity).asDouble() *
                        (strategy.get("4005").asDouble() * itemValue4005 + strategy.get("4004").asDouble() * itemValue4004);
            }
        }

        // 加急许可价值 */
        double itemValue7002;
        switch (itemValueConfigDTO.getExpeditedPlanPricingStrategy()) {
            case "EXPEDITED_PLAN_PRICING_RECRUITMENT_PERMIT":
                itemValue7002 = itemValue7001;
                break;
            default:
                itemValue7002 = itemValueConfigDTO.getExpeditedPlanValue();
        }

        // 家具零件价值 */
        // TODO: 按 SK-5 定价
        double itemValue3401 = itemValueConfigDTO.getFurniturePartValue();

        // 采购凭证价值 = AP-5消耗理智 × (1 - 12 × 龙门币价值) ÷ AP-5 掉落采购凭证数量 */
        double itemValue4006 = 30 * (1 - itemValue4001 * 12) / 21;

        // 芯片助剂价值 = 90 × 采购凭证价值 */
        double itemValue32001 = itemValue4006 * 90;

        // 用公式计算芯片、芯片组价值
        double balancedChipValue = 18 * (1 - itemValue4001 * 12);
        double strongChipValue, weakChipValue;
        WorkshopItemDTO chip = itemValueConfigDTO.getWorkshopStrategy().getChip();
        String chipStrategy = chip.getStrategy();
        double chipByproductRateIncrease = chip.getByproductRateIncrease();

        switch (chipStrategy) {
            case "WORKSHOP_STRATEGY_COMMON":
                double byproductRate = 0.1 * (1 + chipByproductRateIncrease);
                strongChipValue = (6 - byproductRate) / 5 * 18 * (1 - itemValue4001 * 12);
                weakChipValue = (4 + byproductRate) / 5 * 18 * (1 - itemValue4001 * 12);
                break;
            case "WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN":
                strongChipValue = ((6 - 0.1) * 18 * (1 - itemValue4001 * 12) - causalityValue) / 5;
                weakChipValue = ((4 + 0.1) * 18 * (1 - itemValue4001 * 12) + causalityValue) / 5;
                break;
            case "WORKSHOP_STRATEGY_NCDEER_CONSUME":
                strongChipValue = (6 * 18 * (1 - itemValue4001 * 12) + 9 / 10.0 * 36 * causalityValue) / 6;
                weakChipValue = (6 * 18 * (1 - itemValue4001 * 12) - 9 / 10.0 * 36 * causalityValue) / 6;
                break;
            default:
                strongChipValue = balancedChipValue;
                weakChipValue = balancedChipValue;
        }

        // 芯片组

        //均衡芯片价值   36(关卡消耗理智) - 0.0036(龙门币价值)*12(每理智掉落10龙门币*1.2三星作战奖励)*36(关卡消耗理智)
        double balancedChipPackValue = 36 * (1 - itemValue4001 * 12);
        //强势弱势芯片价值
        double strongChipPackValue, weakChipPackValue;
        WorkshopItemDTO chipPack = itemValueConfigDTO.getWorkshopStrategy().getChipPack();
        String chipPackStrategy = chipPack.getStrategy();
        double chipPackByproductRateIncrease = chipPack.getByproductRateIncrease();

        switch (chipPackStrategy) {
            case "WORKSHOP_STRATEGY_COMMON":
                double byproductRate = 0.1 * (1 + chipPackByproductRateIncrease);
                strongChipPackValue = (6 - byproductRate) / 5 * 36 * (1 - itemValue4001 * 12);
                weakChipPackValue = (4 + byproductRate) / 5 * 36 * (1 - itemValue4001 * 12);
                break;
            case "WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN":
                strongChipPackValue = ((6 - 0.1) * 36 * (1 - itemValue4001 * 12) - 2 * causalityValue) / 5;
                weakChipPackValue = ((4 + 0.1) * 36 * (1 - itemValue4001 * 12) + 2 * causalityValue) / 5;
                break;
            case "WORKSHOP_STRATEGY_NCDEER_CONSUME":
                strongChipPackValue = (6 * 36 * (1 - itemValue4001 * 12) + 9 / 10.0 * 36 * causalityValue) / 6;
                weakChipPackValue = (6 * 36 * (1 - itemValue4001 * 12) - 9 / 10.0 * 36 * causalityValue) / 6;
                break;
            default:
                strongChipPackValue = balancedChipPackValue;
                weakChipPackValue = balancedChipPackValue;
        }

        // 双芯片价值
        double balancedDualchipValue = balancedChipPackValue * 2 + itemValue32001 + 1 / 180.0 * itemValueBaseAp;
        double strongDualchipValue = strongChipPackValue * 2 + itemValue32001 + 1 / 180.0 * itemValueBaseAp;
        double weakDualchipValue = weakChipPackValue * 2 + itemValue32001 + 1 / 180.0 * itemValueBaseAp;

        // 模组数据块价值 */
        double itemValueModUnlockToken;
        switch (itemValueConfigDTO.getModUnlockTokenPricingStrategy()) {
            case "MOD_UNLOCK_TOKEN_PRICING_PURCHASE_CERTIFICATE":  // 采购凭证区定价
                itemValueModUnlockToken = 120 * itemValue4006;
                break;
            case "MOD_UNLOCK_TOKEN_PRICING_DISTINCTION_CERTIFICATE":  // 高级凭证区定价
                itemValueModUnlockToken = 20 * itemValue4004;
                break;
            case "MOD_UNLOCK_TOKEN_PRICING_CUSTOM":  // 自定义
                itemValueModUnlockToken = itemValueConfigDTO.getModUnlockTokenValue();
                break;
            default:
                itemValueModUnlockToken = 0;
        }

        // 事相碎片价值 = 20 × 采购凭证价值 */
        double itemValueSTORYREVIEWCOIN = 20 * itemValue4006;

        // 碳素相关价值计算
        double itemValue3114 = 240 / 19.0 * itemValue3401 + 4 * causalityValue - 4000 / 19.0 * itemValue4001;
        double itemValue3113 = 11 / 30.0 * itemValue3114 + 6 / 5.0 * causalityValue;
        double itemValue3112 = 11 / 30.0 * itemValue3113 + 3 / 5.0 * causalityValue;

        // 技巧概要
        SkillSummaryValue skillSummaryValue = calculateSkillSummaryValue(
                itemValueConfigDTO.getWorkshopStrategy().getSkillSummary1to2().getStrategy(),
                itemValueConfigDTO.getWorkshopStrategy().getSkillSummary1to2().getByproductRateIncrease(),
                itemValueConfigDTO.getWorkshopStrategy().getSkillSummary2to3().getStrategy(),
                itemValueConfigDTO.getWorkshopStrategy().getSkillSummary2to3().getByproductRateIncrease(),
                itemValue4001,
                causalityValue
        );

        // 更新物品价值
        itemValueMap.put("EXP", itemValueEXP);
        itemValueMap.put("4003", itemValue4003);
        itemValueMap.put("4002", itemValue4002);
        itemValueMap.put("7003", itemValue7003);
        itemValueMap.put("7004", itemValue7004);
        itemValueMap.put("4005", itemValue4005);
        itemValueMap.put("4004", itemValue4004);
        itemValueMap.put("classic_gacha", itemValueClassicGacha);
        itemValueMap.put("classic_gacha_10", itemValueClassicGacha10);
        itemValueMap.put("3401", itemValue3401);
        itemValueMap.put("7002", itemValue7002);
        itemValueMap.put("4001", itemValue4001);
        itemValueMap.put("2001", itemValue2001);
        itemValueMap.put("2002", itemValue2002);
        itemValueMap.put("2003", itemValue2003);
        itemValueMap.put("2004", itemValue2004);
        itemValueMap.put("base_ap", itemValueBaseAp);
        itemValueMap.put("3003", itemValue3003);
        itemValueMap.put("4006", itemValue4006);
        itemValueMap.put("32001", itemValue32001);
        itemValueMap.put("mod_unlock_token", itemValueModUnlockToken);
        itemValueMap.put("STORY_REVIEW_COIN", itemValueSTORYREVIEWCOIN);
        itemValueMap.put("3114", itemValue3114);
        itemValueMap.put("3113", itemValue3113);
        itemValueMap.put("3112", itemValue3112);
        itemValueMap.put("3303", skillSummaryValue.itemValue3303);
        itemValueMap.put("3302", skillSummaryValue.itemValue3302);
        itemValueMap.put("3301", skillSummaryValue.itemValue3301);
        itemValueMap.put("7001", itemValue7001);

        // 芯片偏好设置
        Map<String, String> chipPreferenceMap = new HashMap<>();
        setupChipPreference(chipPreferenceMap, itemValueConfigDTO);

        // 设置芯片价值
        for (Map.Entry<String, String> entry : chipPreferenceMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            switch (value) {
                case "STRONG":
                    itemValueMap.put("32" + key + "1", strongChipValue);
                    itemValueMap.put("32" + key + "2", strongChipPackValue);
                    itemValueMap.put("32" + key + "3", strongDualchipValue);
                    break;
                case "WEAK":
                    itemValueMap.put("32" + key + "1", weakChipValue);
                    itemValueMap.put("32" + key + "2", weakChipPackValue);
                    itemValueMap.put("32" + key + "3", weakDualchipValue);
                    break;
                case "BALANCED":
                    itemValueMap.put("32" + key + "1", balancedChipValue);
                    itemValueMap.put("32" + key + "2", balancedChipPackValue);
                    itemValueMap.put("32" + key + "3", balancedDualchipValue);
                    break;
            }
        }
    }

    private void calculateEliteMaterialValueFromT3(Map<String, Double> itemValueMap,
                                                   ItemValueConfigDTO itemValueConfigDTO,
                                                   Map<Integer, Double> workshopByproductExpectedValue,
                                                   JsonNode compositeTable,
                                                   Map<String, Double> customItemValueMap) {
        // 龙门币价值 */
        Double lmdValue = itemValueMap.get("4001");

        // 计算因果价值
        // 随机蓝材料的价值 */
        Double t3workShopProductsValue = workshopByproductExpectedValue.get(3);

        // 解构蓝合紫的加工站策略
        WorkshopItemDTO t3toT4Strategy = itemValueConfigDTO.getWorkshopStrategy().getEliteMaterialT3toT4();
        String strategy = t3toT4Strategy.getStrategy();
        Double byproductRateIncrease = t3toT4Strategy.getByproductRateIncrease();

        // 蓝合紫时消耗的龙门币数量 */
        int lmdCost = ("WORKSHOP_STRATEGY_BLEMISHINE".equals(strategy)) ? 0 : 300;

        // 蓝合紫时的实际副产品产出概率 */
        double byproductRate;
        if ("WORKSHOP_STRATEGY_BLEMISHINE".equals(strategy)) {
            byproductRate = 0.14;
        } else {
            byproductRate = 0.1 * (1 + (byproductRateIncrease != null ? byproductRateIncrease : 0));
        }

        // 因果价值 */
        double causalityValue = ((1 - byproductRate) * t3workShopProductsValue - (300 - lmdCost) * lmdValue) / (9.0 / 10.0 * 36);
        // 更新因果价值
        itemValueMap.put("causality", causalityValue);

        // 消耗材料和目标材料的稀有度映射
        Map<Integer, Integer> rarityMap = new HashMap<>();
        rarityMap.put(1, 1);
        rarityMap.put(2, 2);
        rarityMap.put(4, 3);
        rarityMap.put(5, 4);

        for (JsonNode table : compositeTable) {
            // 解构加工路径表：合成或拆解的物品 ID、判断拆解还是合成（resolve === true 为拆解）、合成路径、稀有度
            String itemId = table.get("itemId").asText();
            boolean resolve = table.get("resolve").asBoolean();
            JsonNode pathway = table.get("pathway");
            Integer rarity = table.get("rarity").asInt();

            // 如果这个物品被自定义了，不再通过合成路径得到价值
            if (customItemValueMap.containsKey(itemId)) {
                continue;
            }

            Integer sourceRarity = rarityMap.get(rarity);
            int targetRarity = sourceRarity + 1;

            // 加工消耗的心情 等于 `2 ** (sourceRarity - 1)
            double morale = Math.pow(2, sourceRarity - 1);

            // 获取自定义加工策略和副产品产出概率提升量
            String strategyPropertyName = "eliteMaterialT" + sourceRarity + "toT" + targetRarity;
            WorkshopItemDTO materialStrategy;

            WorkshopStrategyDTO workshopStrategy = itemValueConfigDTO.getWorkshopStrategy();

            switch (strategyPropertyName) {
                case "eliteMaterialT1toT2":
                    materialStrategy = workshopStrategy.getEliteMaterialT1toT2();
                    break;
                case "eliteMaterialT2toT3":
                    materialStrategy = workshopStrategy.getEliteMaterialT2toT3();
                    break;
                case "eliteMaterialT3toT4":
                    materialStrategy = workshopStrategy.getEliteMaterialT3toT4();;
                    break;
                case "eliteMaterialT4toT5":
                    materialStrategy = workshopStrategy.getEliteMaterialT4toT5();
                    break;
                default:
                    continue; // 或者抛出异常
            }

            String currentStrategy = materialStrategy.getStrategy();
            Double currentByproductRateIncrease = materialStrategy.getByproductRateIncrease();

            /*
             实际的副产品产出概率
              - 使用九色鹿获取因果为 `0.1`
              - 使用瑕光为 `0.14`
              - 使用其他干员加工为 `0.1 * (1 + byproductRateIncrease)`
             */
            double currentByproductRate;
            switch (currentStrategy) {
                case "WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN":
                    currentByproductRate = 0.1;
                    break;
                case "WORKSHOP_STRATEGY_BLEMISHINE":
                    currentByproductRate = 0.14;
                    break;
                case "WORKSHOP_STRATEGY_COMMON":
                    currentByproductRate = 0.1 * (1 + (currentByproductRateIncrease != null ? currentByproductRateIncrease : 0));
                    break;
                default:
                    currentByproductRate = 0.1; // 默认值
            }

            //副产品价值的期望
            Double expectedByproductValue = workshopByproductExpectedValue.get(sourceRarity);

            // 加工消耗的龙门币  对于精英材料，不使用瑕光的情况下，消耗的龙门币 = 100 * 原材料稀有度
            int currentLmdCost = ("WORKSHOP_STRATEGY_BLEMISHINE".equals(currentStrategy)) ? 0 : (sourceRarity * 100);

            //期望获取的因果数量 - 使用九色鹿获取因果时，期望获取的因果数量 = 0.9 * 加工消耗的心情

            double expectedCausalityObtained = ("WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN".equals(currentStrategy)) ? (0.9 * morale) : 0;

            // 物品新价值 */
            double newItemValue = 0.0;

            if (resolve) {  // 白、绿色材料是向下拆解
                JsonNode target = pathway.get(0);  // 拆解路径只有一个物品
                // 目标材料价值 */
                Double targetItemValue = itemValueMap.get(target.get("itemId").asText());
                if (targetItemValue == null) targetItemValue = 0.0;
                if (expectedByproductValue == null) expectedByproductValue = 0.0;

                // 消耗材料价值 = (目标材料价值 + 副产品 + 因果（若有） - 龙门币) / 合成所需灰绿材料数量
                newItemValue = (targetItemValue + expectedByproductValue * currentByproductRate + expectedCausalityObtained * causalityValue - currentLmdCost * lmdValue) / target.get("count").asInt();
            } else {
                // 紫、金材料是向上合成
                for (JsonNode cost : pathway) {
                    // 消耗材料价值 */
                    Double sourceItemValue = itemValueMap.get(cost.get("itemId").asText());
                    if (sourceItemValue == null) sourceItemValue = 0.0;
                    newItemValue += sourceItemValue * cost.get("count").asInt();
                }
                // 目标材料价值 = 消耗材料价值之和 + 龙门币 - 副产品
                if (expectedByproductValue == null) expectedByproductValue = 0.0;
                newItemValue += currentLmdCost * lmdValue - expectedByproductValue * currentByproductRate;
            }

            // 更新物品新价值
            itemValueMap.put(itemId, newItemValue);
        }
    }

    private void setupChipPreference(Map<String, String> chipPreferenceMap, ItemValueConfigDTO itemValueConfigDTO) {
        switch (itemValueConfigDTO.getChipPreference().getTANK_MEDIC()) {
            case "TANK":
                chipPreferenceMap.put("3", "STRONG");
                chipPreferenceMap.put("6", "WEAK");
                break;
            case "MEDIC":
                chipPreferenceMap.put("3", "WEAK");
                chipPreferenceMap.put("6", "STRONG");
                break;
            case "BALANCED":
                chipPreferenceMap.put("3", "BALANCED");
                chipPreferenceMap.put("6", "BALANCED");
                break;
        }

        switch (itemValueConfigDTO.getChipPreference().getSNIPER_CASTER()) {
            case "SNIPER":
                chipPreferenceMap.put("4", "STRONG");
                chipPreferenceMap.put("5", "WEAK");
                break;
            case "CASTER":
                chipPreferenceMap.put("4", "WEAK");
                chipPreferenceMap.put("5", "STRONG");
                break;
            case "BALANCED":
                chipPreferenceMap.put("4", "BALANCED");
                chipPreferenceMap.put("5", "BALANCED");
                break;
        }

        switch (itemValueConfigDTO.getChipPreference().getPIONEER_SUPPORT()) {
            case "PIONEER":
                chipPreferenceMap.put("1", "STRONG");
                chipPreferenceMap.put("7", "WEAK");
                break;
            case "SUPPORT":
                chipPreferenceMap.put("1", "WEAK");
                chipPreferenceMap.put("7", "STRONG");
                break;
            case "BALANCED":
                chipPreferenceMap.put("1", "BALANCED");
                chipPreferenceMap.put("7", "BALANCED");
                break;
        }

        switch (itemValueConfigDTO.getChipPreference().getWARRIOR_SPECIAL()) {
            case "WARRIOR":
                chipPreferenceMap.put("2", "STRONG");
                chipPreferenceMap.put("8", "WEAK");
                break;
            case "SPECIAL":
                chipPreferenceMap.put("2", "WEAK");
                chipPreferenceMap.put("8", "STRONG");
                break;
            case "BALANCED":
                chipPreferenceMap.put("2", "BALANCED");
                chipPreferenceMap.put("8", "BALANCED");
                break;
        }
    }

    /**
     * 计算技巧概要的价值
     */
    private SkillSummaryValue calculateSkillSummaryValue(String strategy1to2, double rateIncrease1to2,
                                                         String strategy2to3, double rateIncrease2to3,
                                                         double lmdValue, double causalityValue) {
        double a1 = (strategy1to2.equals("WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN")) ?
                (1.1) : (1 + 0.1 * (1 + rateIncrease1to2));
        double a2 = (strategy2to3.equals("WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN")) ?
                (1.1) : (1 + 0.1 * (1 + rateIncrease2to3));
        double b1 = (strategy1to2.equals("WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN")) ? (0.9) : (0);
        double b2 = (strategy2to3.equals("WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN")) ? (0.9) : (0);

        double itemValue3302 = (30 * (1 - lmdValue * 12) - b1 * causalityValue / 2 + 4 * b2 * causalityValue / a2) /
                (a1 / 2 + 3 / 2.0 + 6 / a2);
        double itemValue3301 = (a1 * itemValue3302 + b1 * causalityValue) / 3;
        double itemValue3303 = (3 * itemValue3302 - 2 * b2 * causalityValue) / a2;

        return new SkillSummaryValue(itemValue3301, itemValue3302, itemValue3303);
    }

    // 辅助类
    private static class SkillSummaryValue {
        public final double itemValue3301;
        public final double itemValue3302;
        public final double itemValue3303;

        public SkillSummaryValue(double itemValue3301, double itemValue3302, double itemValue3303) {
            this.itemValue3301 = itemValue3301;
            this.itemValue3302 = itemValue3302;
            this.itemValue3303 = itemValue3303;
        }
    }


    @RedisCacheable(key = "Json:Penguin_Matrix")

    private Map<String, List<StageInfoAndDropDTO>> getStageInfoAndDropCollect(ItemValueConfigDTO itemValueConfigDTO) {
        String penguinMatrixText = FileUtil.read(ConfigUtil.Penguin + "penguin.json");
        String matrixText = JsonMapper.parseJSONObject(penguinMatrixText).get("matrix").toPrettyString();
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText, new TypeReference<>() {
        });

        String ytlStageDataText = FileUtil.read(ConfigUtil.DataFilePath + "ytl_stage_info.json");

        Map<String, StageInfoAndDropDTO> ytlStageDataMap = JsonMapper.parseJSONArray(ytlStageDataText, new TypeReference<>() {
        });


        Map<String, Stage> stageInfoMap = stageService.getStageInfoMap();


        int sampleSize = itemValueConfigDTO.getSampleSize();

        Set<String> stageBlcklistSet = new HashSet<>();
        if (itemValueConfigDTO.getStageBlacklist() != null) {
            stageBlcklistSet = itemValueConfigDTO.getStageBlacklist();
        }

        Map<String, PenguinMatrixDTO> toughStageMap = penguinMatrixDTOList.stream()
                .filter(item -> item.getStageId().contains("tough"))
                .collect(Collectors
                        .toMap(item -> item.getStageId().replace("tough", "main") + "—" + item.getItemId(), item -> item));

        Map<String, List<StageInfoAndDropDTO>> stageInfoAndDropCollect = new HashMap<>();

        for (PenguinMatrixDTO penguinMatrixDTO : penguinMatrixDTOList) {

            String stageId = penguinMatrixDTO.getStageId();
            String itemId = penguinMatrixDTO.getItemId();
            Integer quantity = penguinMatrixDTO.getQuantity();
            Integer times = penguinMatrixDTO.getTimes();
            Long start = penguinMatrixDTO.getStart();
            Long end = penguinMatrixDTO.getEnd();

            if (stageBlcklistSet.contains(stageId)) {
                continue;
            }

            if ((stageId.contains("main_14") && end != null) || stageId.contains("tough")) {
                continue;
            }

            if (times < sampleSize) {
                continue;
            }

            Stage stageInfo = stageInfoMap.get(stageId);

            if (stageInfo == null) {
                continue;
            }

            //标准关卡的关卡id和物品id的合并id
            String main14StageDropMergeKey = stageId + "—" + itemId;

            //通过标准关卡的关卡id和物品id的合并id获取对应的磨难关卡数据
            PenguinMatrixDTO toughStage = toughStageMap.get(main14StageDropMergeKey);

            //如果没有对应的磨难关卡跳过
            if (toughStage != null) {
                quantity += toughStage.getQuantity();
                times += toughStage.getTimes();

            }

            String stageType = stageInfo.getStageType();
            Integer apCost = stageInfo.getApCost();
            Double spm = stageInfo.getSpm();
            String zoneName = stageInfo.getZoneName();
            String zoneId = stageInfo.getZoneId();
            String stageCode = stageInfo.getStageCode();
            if (StageType.ACT.equals(stageType) && apCost == 21) {
                StageInfoAndDropDTO stageInfoAndDropDTOByItemId = ytlStageDataMap.get(itemId);
                if (stageInfoAndDropDTOByItemId != null) {
                    stageInfoAndDropDTOByItemId.setQuantity(stageInfoAndDropDTOByItemId.getQuantity() + quantity);
                    stageInfoAndDropDTOByItemId.setTimes(stageInfoAndDropDTOByItemId.getTimes() + times);
                }
            }

            StageInfoAndDropDTO stageInfoAndDropDTO = new StageInfoAndDropDTO();
            stageInfoAndDropDTO.setStageId(stageId);
            stageInfoAndDropDTO.setStageCode(stageCode);
            stageInfoAndDropDTO.setItemId(itemId);
            stageInfoAndDropDTO.setQuantity(quantity);
            stageInfoAndDropDTO.setTimes(times);
            stageInfoAndDropDTO.setStart(start);
            stageInfoAndDropDTO.setEnd(end);
            stageInfoAndDropDTO.setStageType(stageType);
            stageInfoAndDropDTO.setZoneName(zoneName);
            stageInfoAndDropDTO.setZoneId(zoneId);
            stageInfoAndDropDTO.setApCost(apCost);
            stageInfoAndDropDTO.setSpm(spm);


            if (!stageInfoAndDropCollect.containsKey(stageId)) {
                List<StageInfoAndDropDTO> stageInfoAndDropDTOList = new ArrayList<>();
                StageInfoAndDropDTO stageInfoAndLMDDrop = getStageInfoAndLMDDrop(stageInfoAndDropDTO, 12);
                stageInfoAndDropDTOList.add(stageInfoAndLMDDrop);
                if (StageType.ACT.equals(stageType) || StageType.ACT_REP.equals(stageType)) {
                    StageInfoAndDropDTO activityShopLMDExchange = getStageInfoAndLMDDrop(stageInfoAndDropDTO, 20);
                    stageInfoAndDropDTOList.add(activityShopLMDExchange);
                }
                stageInfoAndDropCollect.put(stageId, stageInfoAndDropDTOList);
            }

            stageInfoAndDropCollect.get(stageId).add(stageInfoAndDropDTO);
        }

        if (itemValueConfigDTO.getUseActivityAverageStage()) {
            for (StageInfoAndDropDTO stageInfoAndDropDTO : ytlStageDataMap.values()) {
                StageInfoAndDropDTO stageInfoAndLMDDrop = getStageInfoAndLMDDrop(stageInfoAndDropDTO, 12);
                StageInfoAndDropDTO activityShopLMDExchange = getStageInfoAndLMDDrop(stageInfoAndDropDTO, 20);
                List<StageInfoAndDropDTO> stageInfoAndDropDTOList = new ArrayList<>();
                stageInfoAndDropDTOList.add(stageInfoAndDropDTO);
                stageInfoAndDropDTOList.add(stageInfoAndLMDDrop);
                stageInfoAndDropDTOList.add(activityShopLMDExchange);
                stageInfoAndDropCollect.put(stageInfoAndDropDTO.getStageId(), stageInfoAndDropDTOList);
            }
        }

        return stageInfoAndDropCollect;
    }

    private StageInfoAndDropDTO getStageInfoAndLMDDrop(StageInfoAndDropDTO dto, Integer LMDPerAp) {
        StageInfoAndDropDTO stageInfoAndDropDTO = new StageInfoAndDropDTO();
        stageInfoAndDropDTO.setStageId(dto.getStageId());
        stageInfoAndDropDTO.setStageCode(dto.getStageCode());
        stageInfoAndDropDTO.setZoneName(dto.getZoneName());
        stageInfoAndDropDTO.setZoneId(dto.getZoneId());
        stageInfoAndDropDTO.setApCost(dto.getApCost());
        stageInfoAndDropDTO.setSpm(dto.getSpm());
        stageInfoAndDropDTO.setStageType(dto.getStageType());
        stageInfoAndDropDTO.setStart(dto.getStart());
        stageInfoAndDropDTO.setEnd(dto.getEnd());
        stageInfoAndDropDTO.setQuantity(dto.getApCost() * LMDPerAp);
        stageInfoAndDropDTO.setTimes(1);
        stageInfoAndDropDTO.setItemId("4001");

        return stageInfoAndDropDTO;
    }

    private void checkItemValueConfig(ItemValueConfigDTO configDTO) {
        checkNotNull(configDTO.getUseActivityAverageStage());
        checkNotNull(configDTO.getSampleSize());
        checkNotNull(configDTO.getStageBlacklist());
        checkNotNull(configDTO.getStageWhitelist());
        checkNotNull(configDTO.getKernelHeadhuntingPermitPricingStrategy());
        checkNotNull(configDTO.getKernelHeadhuntingPermitCoefficient());
        checkNotNull(configDTO.getLmdPricingStrategy());
        checkNotNull(configDTO.getLmdCoefficient());
        checkNotNull(configDTO.getExpPricingStrategy());
        checkNotNull(configDTO.getExpCoefficient());
        checkNotNull(configDTO.getModUnlockTokenPricingStrategy());
        checkNotNull(configDTO.getModUnlockTokenValue());
        checkNotNull(configDTO.getRecruitmentPermitPricingStrategy());
        checkNotNull(configDTO.getRecruitmentPermitValue());
        checkNotNull(configDTO.getExpeditedPlanPricingStrategy());
        checkNotNull(configDTO.getExpeditedPlanValue());
        checkNotNull(configDTO.getFurniturePartPricingStrategy());
        checkNotNull(configDTO.getFurniturePartValue());
        checkNotNull(configDTO.getCustomItem());
        checkNotNull(configDTO.getWorkshopStrategy());
        checkNotNull(configDTO.getChipPreference());

        if (configDTO.getOrundumPricingStrategy() == null && configDTO.getOrundumValue() == null) {
            configDTO.setOrundumValue(0.75);
        }

        if (configDTO.getOrundumValue() <= 0) {
            throw new ServiceException(ResultCode.ORUNDUM_VALUE_CANNOT_BE_LESS_THAN_0);
        }

        if (configDTO.getOriginitePrimePricingStrategy() == null && configDTO.getOriginitePrimeCoefficient() == null) {
            configDTO.setOrundumValue(180.0);
        }

        if (configDTO.getOriginitePrimeCoefficient() <= 0) {
            throw new ServiceException(ResultCode.ORIGINITE_PRIME_VALUE_CANNOT_BE_LESS_THAN_0);
        }

        if (configDTO.getSampleSize() == null) {
            configDTO.setSampleSize(300);
        }

        if (configDTO.getSampleSize() <= 50) {
            throw new ServiceException(ResultCode.SAMPLE_SIZE_CANNOT_BE_ZERO_OR_LESS_THAN_50);
        }


    }


    private static void checkNotNull(Object value) {
        if (value == null) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }
    }


    @RedisCacheable(key = "Json:Recruitment_Table")
    private JsonNode getRecruitmentTable() {
        return JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "recruitment_table.json"));
    }

    @RedisCacheable(key = "Json:Item_Info")
    private JsonNode getItemInfo() {
        return JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "itemInfo.json"));
    }

}
