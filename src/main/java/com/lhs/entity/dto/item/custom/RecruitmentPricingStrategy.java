package com.lhs.entity.dto.item.custom;

/**
 * 单个稀有度公开招募的凭证定价策略
 */
public class RecruitmentPricingStrategy {

    private final double cert4005;
    private final double cert4004;

    public RecruitmentPricingStrategy(double cert4005, double cert4004) {
        this.cert4005 = cert4005;
        this.cert4004 = cert4004;
    }

    /**
     * 资质凭证数量
     */
    public double getCert4005() {
        return cert4005;
    }

    /**
     * 高级凭证数量
     */
    public double getCert4004() {
        return cert4004;
    }
}
