package com.lhs.entity.vo.material;

import java.util.List;

/**
 * 关卡效率结果 VO
 * 对应前端 stageEfficiencyCal.js 中 calculationStageEfficiency 返回的单条关卡结果
 */
public class StageEfficiencyVO {

    /** 关卡名称 */
    private String stageCode;
    /** 关卡ID */
    private String stageId;
    /** 关卡类型 */
    private String stageType;
    /** 所属章节 */
    private String zoneName;
    /** 主产物名称 */
    private String itemName;
    /** 主产物ID */
    private String itemId;
    /** 主产物所属材料系列ID */
    private String itemSeriesId;
    /** 主产物稀有度 */
    private Integer itemRarity;
    /** 副产物ID */
    private String secondaryItemId;
    /** 主产物期望理智 */
    private Double apExpect;
    /** 主产物掉率 */
    private Double knockRating;
    /** 主产物样本数 */
    private Long sampleSize;
    /** 关卡总理智转化率 */
    private Double stageEfficiency;
    /** 仅计算该系列 T1-T2 材料的理智转化率 */
    private Double leT2Efficiency;
    /** 仅计算该系列 T1-T3 材料的理智转化率 */
    private Double leT3Efficiency;
    /** 仅计算该系列 T1-T4 材料的理智转化率 */
    private Double leT4Efficiency;
    /** 不考虑其他消耗每理智可转化的合成玉数量 */
    private Double orundumPerAp;
    /** 搓玉效率（以最高搓玉关卡为 1） */
    private Double orundumPerApEfficiency;
    /** 每搓一抽合成玉需要的龙门币消耗（单位：万） */
    private Double lmdCost;
    /** 关卡结束时间戳 */
    private Long end;
    /** 每分钟消耗理智 */
    private Double spm;
    /** 关卡消耗理智 */
    private Integer apCost;
    /** 关卡期望产出总价值 */
    private Double stageExpectedOutput;
    /** 掉落明细 */
    private List<StageDropDetailVO> dropDetail;

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getStageType() {
        return stageType;
    }

    public void setStageType(String stageType) {
        this.stageType = stageType;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemSeriesId() {
        return itemSeriesId;
    }

    public void setItemSeriesId(String itemSeriesId) {
        this.itemSeriesId = itemSeriesId;
    }

    public Integer getItemRarity() {
        return itemRarity;
    }

    public void setItemRarity(Integer itemRarity) {
        this.itemRarity = itemRarity;
    }

    public String getSecondaryItemId() {
        return secondaryItemId;
    }

    public void setSecondaryItemId(String secondaryItemId) {
        this.secondaryItemId = secondaryItemId;
    }

    public Double getApExpect() {
        return apExpect;
    }

    public void setApExpect(Double apExpect) {
        this.apExpect = apExpect;
    }

    public Double getKnockRating() {
        return knockRating;
    }

    public void setKnockRating(Double knockRating) {
        this.knockRating = knockRating;
    }

    public Long getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Long sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Double getStageEfficiency() {
        return stageEfficiency;
    }

    public void setStageEfficiency(Double stageEfficiency) {
        this.stageEfficiency = stageEfficiency;
    }

    public Double getLeT2Efficiency() {
        return leT2Efficiency;
    }

    public void setLeT2Efficiency(Double leT2Efficiency) {
        this.leT2Efficiency = leT2Efficiency;
    }

    public Double getLeT3Efficiency() {
        return leT3Efficiency;
    }

    public void setLeT3Efficiency(Double leT3Efficiency) {
        this.leT3Efficiency = leT3Efficiency;
    }

    public Double getLeT4Efficiency() {
        return leT4Efficiency;
    }

    public void setLeT4Efficiency(Double leT4Efficiency) {
        this.leT4Efficiency = leT4Efficiency;
    }

    public Double getOrundumPerAp() {
        return orundumPerAp;
    }

    public void setOrundumPerAp(Double orundumPerAp) {
        this.orundumPerAp = orundumPerAp;
    }

    public Double getOrundumPerApEfficiency() {
        return orundumPerApEfficiency;
    }

    public void setOrundumPerApEfficiency(Double orundumPerApEfficiency) {
        this.orundumPerApEfficiency = orundumPerApEfficiency;
    }

    public Double getLmdCost() {
        return lmdCost;
    }

    public void setLmdCost(Double lmdCost) {
        this.lmdCost = lmdCost;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public Double getSpm() {
        return spm;
    }

    public void setSpm(Double spm) {
        this.spm = spm;
    }

    public Integer getApCost() {
        return apCost;
    }

    public void setApCost(Integer apCost) {
        this.apCost = apCost;
    }

    public Double getStageExpectedOutput() {
        return stageExpectedOutput;
    }

    public void setStageExpectedOutput(Double stageExpectedOutput) {
        this.stageExpectedOutput = stageExpectedOutput;
    }

    public List<StageDropDetailVO> getDropDetail() {
        return dropDetail;
    }

    public void setDropDetail(List<StageDropDetailVO> dropDetail) {
        this.dropDetail = dropDetail;
    }
}
