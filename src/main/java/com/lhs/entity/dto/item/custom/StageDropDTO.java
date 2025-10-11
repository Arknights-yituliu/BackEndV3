package com.lhs.entity.dto.item.custom;

public class StageDropDTO {

    private String stageId;
    private String itemId;
    private Integer quantity;
    private Integer times;
    private String start;
    private String end;
    private String stageCode;
    private Integer apCost;
    private Double spm;
    private String stageType;
    private String zoneName;
    private String zoneId;

    public StageDropDTO() {
    }

    public StageDropDTO(String stageId, String itemId, Integer quantity, Integer times, String start, String end, String stageCode, Integer apCost, Double spm, String stageType, String zoneName, String zoneId) {
        this.stageId = stageId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.times = times;
        this.start = start;
        this.end = end;
        this.stageCode = stageCode;
        this.apCost = apCost;
        this.spm = spm;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public Integer getApCost() {
        return apCost;
    }

    public void setApCost(Integer apCost) {
        this.apCost = apCost;
    }

    public Double getSpm() {
        return spm;
    }

    public void setSpm(Double spm) {
        this.spm = spm;
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

    @Override
    public String toString() {
        return "PenguinMatrix{" +
                "stageId='" + stageId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", quantity=" + quantity +
                ", times=" + times +
                ", start='" + start + '\'' +
                ", end='" + end + '\'' +
                ", stageCode='" + stageCode + '\'' +
                ", apCost=" + apCost +
                ", spm=" + spm +
                ", stageType='" + stageType + '\'' +
                ", zoneName='" + zoneName + '\'' +
                ", zoneId='" + zoneId + '\'' +
                '}';
    }
}
