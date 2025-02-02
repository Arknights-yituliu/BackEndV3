package com.lhs.entity.dto.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class StageConfigDTO {

    private Long id;
    //API版本
    private String version;
    //经验书的系数,经验书价值=龙门币(0.0036)*系数
    private Double expCoefficient;
    //样本量
    private Integer sampleSize;
    //龙门币系数   龙门币价值 =  根据钱本计算的龙门币价值(0.0036) * 龙门币系数
    private Double lmdCoefficient;
    //是否计算活动关卡
    private Boolean useActivityStage;
    //芯片是否按均价计算
    private Boolean chipIsValueConsistent;
    //关卡黑名单，计算中不使用这些关卡
    private List<StageBlacklistDTO> stageBlacklist;
    //强制指定某个材料的价值（例如无限池扭转醇）
    private List<ItemCustomValueDTO> customItem;
    private Long updateTime;

    {
        id = 202412050002L;
        version = "v4";
        expCoefficient = 0.633;
        chipIsValueConsistent = true;
        useActivityStage = false;
        sampleSize = 300;
        lmdCoefficient = 1.0;
        stageBlacklist =  new ArrayList<>();
    }

    //关卡黑名单字典
    public Map<String, String> getStageBlackMap() {
        Map<String, String> stageBlackMap = new HashMap<>();
        for( StageBlacklistDTO stage: this.stageBlacklist){
            stageBlackMap.put(stage.getStageId(),stage.getStageCode());
        }
        return stageBlackMap;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Double getExpCoefficient() {
        return expCoefficient;
    }

    public void setExpCoefficient(Double expCoefficient) {
        this.expCoefficient = expCoefficient;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Double getLmdCoefficient() {
        return lmdCoefficient;
    }

    public void setLmdCoefficient(Double lmdCoefficient) {
        this.lmdCoefficient = lmdCoefficient;
    }

    public Boolean getUseActivityStage() {
        return useActivityStage;
    }

    public void setUseActivityStage(Boolean useActivityStage) {
        this.useActivityStage = useActivityStage;
    }

    public Boolean getChipIsValueConsistent() {
        return chipIsValueConsistent;
    }

    public void setChipIsValueConsistent(Boolean chipIsValueConsistent) {
        this.chipIsValueConsistent = chipIsValueConsistent;
    }

    public List<StageBlacklistDTO> getStageBlacklist() {
        return stageBlacklist;
    }

    public void setStageBlacklist(List<StageBlacklistDTO> stageBlacklist) {
        this.stageBlacklist = stageBlacklist;
    }

    public List<ItemCustomValueDTO> getCustomItem() {
        return customItem;
    }

    public void setCustomItem(List<ItemCustomValueDTO> customItem) {
        this.customItem = customItem;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Double getLMDValue() {
        return 0.0036 * lmdCoefficient;
    }

    public Double getEXPValue() {
        return 0.0036 * expCoefficient;
    }

    //  返回版本号
    public String getVersionCode() {
        return version + "-" + id;
    }

    @Override
    public String toString() {
        return "StageConfigDTO{" +
                "id=" + id +
                ", version='" + version + '\'' +
                ", expCoefficient=" + expCoefficient +
                ", sampleSize=" + sampleSize +
                ", lmdCoefficient=" + lmdCoefficient +
                ", userActivityStage=" + useActivityStage +
                ", chipIsValueConsistent=" + chipIsValueConsistent +
                ", stageBlacklist=" + stageBlacklist +
                ", customItem=" + customItem +
                ", updateTime=" + updateTime +
                '}';
    }
}
