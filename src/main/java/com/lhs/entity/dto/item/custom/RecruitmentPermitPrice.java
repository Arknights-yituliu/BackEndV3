package com.lhs.entity.dto.item.custom;

import java.util.Map;

public class RecruitmentPermitPrice {

    private Map<String, Map<String,Integer>> recruitmentPermitPricing_3_4;
    private Map<String, Map<String,Integer>> recruitmentPermitPricing_3_4_5;
    private Map<String, Map<String,Integer>> recruitmentPermitPricing_3_4_5_6;

    public Map<String, Map<String, Integer>> getRecruitmentPermitPricing_3_4() {
        return recruitmentPermitPricing_3_4;
    }

    public void setRecruitmentPermitPricing_3_4(Map<String, Map<String, Integer>> recruitmentPermitPricing_3_4) {
        this.recruitmentPermitPricing_3_4 = recruitmentPermitPricing_3_4;
    }

    public Map<String, Map<String, Integer>> getRecruitmentPermitPricing_3_4_5() {
        return recruitmentPermitPricing_3_4_5;
    }

    public void setRecruitmentPermitPricing_3_4_5(Map<String, Map<String, Integer>> recruitmentPermitPricing_3_4_5) {
        this.recruitmentPermitPricing_3_4_5 = recruitmentPermitPricing_3_4_5;
    }

    public Map<String, Map<String, Integer>> getRecruitmentPermitPricing_3_4_5_6() {
        return recruitmentPermitPricing_3_4_5_6;
    }

    public void setRecruitmentPermitPricing_3_4_5_6(Map<String, Map<String, Integer>> recruitmentPermitPricing_3_4_5_6) {
        this.recruitmentPermitPricing_3_4_5_6 = recruitmentPermitPricing_3_4_5_6;
    }

    @Override
    public String toString() {
        return "RecruitmentPermitPrice{" +
                "recruitmentPermitPricing_3_4=" + recruitmentPermitPricing_3_4 +
                ", recruitmentPermitPricing_3_4_5=" + recruitmentPermitPricing_3_4_5 +
                ", recruitmentPermitPricing_3_4_5_6=" + recruitmentPermitPricing_3_4_5_6 +
                '}';
    }
}
