package com.lhs.entity.po.material;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.poi.hpsf.Date;

@TableName
public class StageEfficiency {

    @TableId
    private Long id;
    private String stageId;
    private String stageCode;
    private String stageType;

    private String zoneName;
    private String zoneId;
    private String itemName;
    private String itemId;
    private Integer itemRarity;
    private String secondaryItemId;
    private Double knockRating;
    private Integer sampleSize;
    private Double stageEfficiency;
    private Double leT2Efficiency;
    private Double leT3Efficiency;
    private Double leT4Efficiency;
    private Double orundumPerAp;
    private Double lmdcost;
    private Date endTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
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

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
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

    public Double getKnockRating() {
        return knockRating;
    }

    public void setKnockRating(Double knockRating) {
        this.knockRating = knockRating;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
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

    public Double getLmdcost() {
        return lmdcost;
    }

    public void setLmdcost(Double lmdcost) {
        this.lmdcost = lmdcost;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
