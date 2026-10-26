package com.lhs.entity.vo.material;

/**
 * 关卡掉落明细 VO
 * 对应前端 stageEfficiencyCal.js 中 dropDetail 的每一项
 */
public class StageDropDetailVO {

    /** 物品名称 */
    private String itemName;
    /** 物品ID */
    private String itemId;
    /** 物品价值 */
    private Double itemValue;
    /** 期望产出价值 */
    private Double expectedOutput;
    /** 材料掉率（掉落数量 / 样本数） */
    private Double knockRating;
    /** 是否为无限池兑换物的虚拟掉落 */
    private Boolean isUnlimited;
    /** 稀有度 */
    private Integer rarity;
    /** 掉落数量 */
    private Long quantity;
    /** 样本数 */
    private Long times;
    /** 样本数（与 times 相同，保留前端字段） */
    private Long sampleSize;
    /** 关卡结束时间戳 */
    private Long end;

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

    public Double getItemValue() {
        return itemValue;
    }

    public void setItemValue(Double itemValue) {
        this.itemValue = itemValue;
    }

    public Double getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(Double expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public Double getKnockRating() {
        return knockRating;
    }

    public void setKnockRating(Double knockRating) {
        this.knockRating = knockRating;
    }

    public Boolean getIsUnlimited() {
        return isUnlimited;
    }

    public void setIsUnlimited(Boolean isUnlimited) {
        this.isUnlimited = isUnlimited;
    }

    public Integer getRarity() {
        return rarity;
    }

    public void setRarity(Integer rarity) {
        this.rarity = rarity;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getTimes() {
        return times;
    }

    public void setTimes(Long times) {
        this.times = times;
    }

    public Long getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Long sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }
}
