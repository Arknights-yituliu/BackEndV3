package com.lhs.common.util;

import java.util.HashMap;
import java.util.Map;

import com.lhs.entity.dto.material.RecruitmentPricingStrategy;

/**
 * 招募定价工具类，数据写死在类中，无需读取 JSON
 */
public class ItemValueUtil {

    private static final Map<Integer, Double> OPERATOR_RATES = new HashMap<>();
    private static final Map<String, Map<Integer, RecruitmentPricingStrategy>> PRICING_STRATEGIES = new HashMap<>();

    static {
        // 公开招募干员概率
        OPERATOR_RATES.put(3, 0.7882);
        OPERATOR_RATES.put(4, 0.2027);
        OPERATOR_RATES.put(5, 0.0069);
        OPERATOR_RATES.put(6, 0.0022);

        // 定价策略: recruitmentPermitPricing_3_4
        Map<Integer, RecruitmentPricingStrategy> pricing3_4 = new HashMap<>();
        pricing3_4.put(3, new RecruitmentPricingStrategy(10, 0));
        pricing3_4.put(4, new RecruitmentPricingStrategy(30, 1));
        pricing3_4.put(5, new RecruitmentPricingStrategy(0, 5));
        pricing3_4.put(6, new RecruitmentPricingStrategy(0, 10));
        PRICING_STRATEGIES.put("recruitmentPermitPricing_3_4", pricing3_4);

        // 定价策略: recruitmentPermitPricing_3_4_5
        Map<Integer, RecruitmentPricingStrategy> pricing3_4_5 = new HashMap<>();
        pricing3_4_5.put(3, new RecruitmentPricingStrategy(10, 0));
        pricing3_4_5.put(4, new RecruitmentPricingStrategy(30, 1));
        pricing3_4_5.put(5, new RecruitmentPricingStrategy(0, 13));
        pricing3_4_5.put(6, new RecruitmentPricingStrategy(0, 10));
        PRICING_STRATEGIES.put("recruitmentPermitPricing_3_4_5", pricing3_4_5);

        // 定价策略: recruitmentPermitPricing_3_4_5_6
        Map<Integer, RecruitmentPricingStrategy> pricing3_4_5_6 = new HashMap<>();
        pricing3_4_5_6.put(3, new RecruitmentPricingStrategy(10, 0));
        pricing3_4_5_6.put(4, new RecruitmentPricingStrategy(30, 1));
        pricing3_4_5_6.put(5, new RecruitmentPricingStrategy(0, 13));
        pricing3_4_5_6.put(6, new RecruitmentPricingStrategy(0, 25));
        PRICING_STRATEGIES.put("recruitmentPermitPricing_3_4_5_6", pricing3_4_5_6);
    }

    private ItemValueUtil() {
    }

    /**
     * 获取公开招募干员概率 Map（只读）
     * @return 稀有度 → 概率 的不可变映射
     */
    public static Map<Integer, Double> getOperatorRecruitmentRates() {
        return Map.copyOf(OPERATOR_RATES);
    }

    /**
     * 获取招聘许可定价 Map（只读）
     * @return 策略名称 → 稀有度定价 的不可变映射
     */
    public static Map<String, Map<Integer, RecruitmentPricingStrategy>> getRecruitmentPermitPricing() {
        return Map.copyOf(PRICING_STRATEGIES);
    }

    /**
     * 获取招聘许可干员的公开招募出现概率
     * @param rarity 稀有度 3~6
     * @return 出现概率
     */
    public static double getOperatorRecruitmentRate(int rarity) {
        return OPERATOR_RATES.getOrDefault(rarity, 0.0);
    }

    /**
     * 获取指定策略下指定稀有度的定价方案
     * @param strategy 策略名称
     * @param rarity 稀有度 3~6
     * @return 稀有度定价，若不存在则返回 (0, 0)
     */
    public static RecruitmentPricingStrategy getPricing(String strategy, int rarity) {
        Map<Integer, RecruitmentPricingStrategy> rarityMap = PRICING_STRATEGIES.get(strategy);
        if (rarityMap == null) {
            return new RecruitmentPricingStrategy(0, 0);
        }
        return rarityMap.getOrDefault(rarity, new RecruitmentPricingStrategy(0, 0));
    }

    /**
     * 计算招聘许可的期望价值
     * @param strategy 定价策略名称
     * @param cert4005Value 资质凭证单价
     * @param cert4004Value 高级凭证单价
     * @return 招聘许可期望价值
     */
    public static double calculateRecruitmentPermitValue(String strategy, double cert4005Value, double cert4004Value) {
        double value = 0;
        for (int rarity = 3; rarity <= 6; rarity++) {
            double rate = getOperatorRecruitmentRate(rarity);
            RecruitmentPricingStrategy pricing = getPricing(strategy, rarity);
            value += rate * (pricing.getCert4005() * cert4005Value
                    + pricing.getCert4004() * cert4004Value);
        }
        return value;
    }
}
