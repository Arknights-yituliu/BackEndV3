package com.lhs.entity.dto.item;

import com.lhs.entity.dto.material.PenguinMatrixDTO;

import java.util.List;

public class StageInfoAndDrop {

    private String stageId;
    private String stageCode;
    private String zoneId;
    private String zoneName;
    private Integer apCost;
    private String stageType;

    private List<PenguinMatrixDTO> dropList;


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

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public Integer getApCost() {
        return apCost;
    }

    public void setApCost(Integer apCost) {
        this.apCost = apCost;
    }

    public String getStageType() {
        return stageType;
    }

    public void setStageType(String stageType) {
        this.stageType = stageType;
    }

    public List<PenguinMatrixDTO> getDropList() {
        return dropList;
    }

    public void setDropList(List<PenguinMatrixDTO> dropList) {
        this.dropList = dropList;
    }
}
