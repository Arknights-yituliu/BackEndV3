package com.lhs.entity.dto.material;

import java.util.List;
import java.util.Set;

public class ItemValueConfigDTO {
    private String source;
    private String version;
    private Boolean useActivityAverageStage;
    private Boolean useActivityAverageStageAndUnlimitedItem;
    private Integer sampleSize;
    private Set<String> stageBlacklist;
    private Set<String> stageWhitelist;
    private String orundumPricingStrategy;
    private Double orundumValue;
    private String originitePrimePricingStrategy;
    private Double originitePrimeCoefficient;
    private String kernelHeadhuntingPermitPricingStrategy;
    private Double kernelHeadhuntingPermitCoefficient;
    private String lmdPricingStrategy;
    private Double lmdCoefficient;
    private String expPricingStrategy;
    private Double expCoefficient;
    private String modUnlockTokenPricingStrategy;
    private Double modUnlockTokenValue;
    private String recruitmentPermitPricingStrategy;
    private Double recruitmentPermitValue;
    private String furniturePartPricingStrategy;
    private Double furniturePartValue;
    private List<CustomItemDTO> customItem;
    private WorkshopStrategyDTO workshopStrategy;
    private ChipPreferenceDTO chipPreference;

    public ItemValueConfigDTO() {
    }

    public ItemValueConfigDTO(String source, String version, Boolean useActivityAverageStage, Boolean useActivityAverageStageAndUnlimitedItem, Integer sampleSize, Set<String> stageBlacklist, Set<String> stageWhitelist, String orundumPricingStrategy, Double orundumValue, String originitePrimePricingStrategy, Double originitePrimeCoefficient, String kernelHeadhuntingPermitPricingStrategy, Double kernelHeadhuntingPermitCoefficient, String lmdPricingStrategy, Double lmdCoefficient, String expPricingStrategy, Double expCoefficient, String modUnlockTokenPricingStrategy, Double modUnlockTokenValue, String recruitmentPermitPricingStrategy, Double recruitmentPermitValue, String furniturePartPricingStrategy, Double furniturePartValue, List<CustomItemDTO> customItem, WorkshopStrategyDTO workshopStrategy, ChipPreferenceDTO chipPreference) {
        this.source = source;
        this.version = version;
        this.useActivityAverageStage = useActivityAverageStage;
        this.useActivityAverageStageAndUnlimitedItem = useActivityAverageStageAndUnlimitedItem;
        this.sampleSize = sampleSize;
        this.stageBlacklist = stageBlacklist;
        this.stageWhitelist = stageWhitelist;
        this.orundumPricingStrategy = orundumPricingStrategy;
        this.orundumValue = orundumValue;
        this.originitePrimePricingStrategy = originitePrimePricingStrategy;
        this.originitePrimeCoefficient = originitePrimeCoefficient;
        this.kernelHeadhuntingPermitPricingStrategy = kernelHeadhuntingPermitPricingStrategy;
        this.kernelHeadhuntingPermitCoefficient = kernelHeadhuntingPermitCoefficient;
        this.lmdPricingStrategy = lmdPricingStrategy;
        this.lmdCoefficient = lmdCoefficient;
        this.expPricingStrategy = expPricingStrategy;
        this.expCoefficient = expCoefficient;
        this.modUnlockTokenPricingStrategy = modUnlockTokenPricingStrategy;
        this.modUnlockTokenValue = modUnlockTokenValue;
        this.recruitmentPermitPricingStrategy = recruitmentPermitPricingStrategy;
        this.recruitmentPermitValue = recruitmentPermitValue;
        this.furniturePartPricingStrategy = furniturePartPricingStrategy;
        this.furniturePartValue = furniturePartValue;
        this.customItem = customItem;
        this.workshopStrategy = workshopStrategy;
        this.chipPreference = chipPreference;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getUseActivityAverageStage() {
        return useActivityAverageStage;
    }

    public void setUseActivityAverageStage(Boolean useActivityAverageStage) {
        this.useActivityAverageStage = useActivityAverageStage;
    }

    public Boolean getUseActivityAverageStageAndUnlimitedItem() {
        return useActivityAverageStageAndUnlimitedItem;
    }

    public void setUseActivityAverageStageAndUnlimitedItem(Boolean useActivityAverageStageAndUnlimitedItem) {
        this.useActivityAverageStageAndUnlimitedItem = useActivityAverageStageAndUnlimitedItem;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Set<String> getStageBlacklist() {
        return stageBlacklist;
    }

    public void setStageBlacklist(Set<String> stageBlacklist) {
        this.stageBlacklist = stageBlacklist;
    }

    public Set<String> getStageWhitelist() {
        return stageWhitelist;
    }

    public void setStageWhitelist(Set<String> stageWhitelist) {
        this.stageWhitelist = stageWhitelist;
    }

    public String getOrundumPricingStrategy() {
        return orundumPricingStrategy;
    }

    public void setOrundumPricingStrategy(String orundumPricingStrategy) {
        this.orundumPricingStrategy = orundumPricingStrategy;
    }

    public Double getOrundumValue() {
        return orundumValue;
    }

    public void setOrundumValue(Double orundumValue) {
        this.orundumValue = orundumValue;
    }

    public String getOriginitePrimePricingStrategy() {
        return originitePrimePricingStrategy;
    }

    public void setOriginitePrimePricingStrategy(String originitePrimePricingStrategy) {
        this.originitePrimePricingStrategy = originitePrimePricingStrategy;
    }

    public Double getOriginitePrimeCoefficient() {
        return originitePrimeCoefficient;
    }

    public void setOriginitePrimeCoefficient(Double originitePrimeCoefficient) {
        this.originitePrimeCoefficient = originitePrimeCoefficient;
    }

    public String getKernelHeadhuntingPermitPricingStrategy() {
        return kernelHeadhuntingPermitPricingStrategy;
    }

    public void setKernelHeadhuntingPermitPricingStrategy(String kernelHeadhuntingPermitPricingStrategy) {
        this.kernelHeadhuntingPermitPricingStrategy = kernelHeadhuntingPermitPricingStrategy;
    }

    public Double getKernelHeadhuntingPermitCoefficient() {
        return kernelHeadhuntingPermitCoefficient;
    }

    public void setKernelHeadhuntingPermitCoefficient(Double kernelHeadhuntingPermitCoefficient) {
        this.kernelHeadhuntingPermitCoefficient = kernelHeadhuntingPermitCoefficient;
    }

    public String getLmdPricingStrategy() {
        return lmdPricingStrategy;
    }

    public void setLmdPricingStrategy(String lmdPricingStrategy) {
        this.lmdPricingStrategy = lmdPricingStrategy;
    }

    public Double getLmdCoefficient() {
        return lmdCoefficient;
    }

    public void setLmdCoefficient(Double lmdCoefficient) {
        this.lmdCoefficient = lmdCoefficient;
    }

    public String getExpPricingStrategy() {
        return expPricingStrategy;
    }

    public void setExpPricingStrategy(String expPricingStrategy) {
        this.expPricingStrategy = expPricingStrategy;
    }

    public Double getExpCoefficient() {
        return expCoefficient;
    }

    public void setExpCoefficient(Double expCoefficient) {
        this.expCoefficient = expCoefficient;
    }

    public String getModUnlockTokenPricingStrategy() {
        return modUnlockTokenPricingStrategy;
    }

    public void setModUnlockTokenPricingStrategy(String modUnlockTokenPricingStrategy) {
        this.modUnlockTokenPricingStrategy = modUnlockTokenPricingStrategy;
    }

    public Double getModUnlockTokenValue() {
        return modUnlockTokenValue;
    }

    public void setModUnlockTokenValue(Double modUnlockTokenValue) {
        this.modUnlockTokenValue = modUnlockTokenValue;
    }

    public String getRecruitmentPermitPricingStrategy() {
        return recruitmentPermitPricingStrategy;
    }

    public void setRecruitmentPermitPricingStrategy(String recruitmentPermitPricingStrategy) {
        this.recruitmentPermitPricingStrategy = recruitmentPermitPricingStrategy;
    }

    public Double getRecruitmentPermitValue() {
        return recruitmentPermitValue;
    }

    public void setRecruitmentPermitValue(Double recruitmentPermitValue) {
        this.recruitmentPermitValue = recruitmentPermitValue;
    }



    public String getFurniturePartPricingStrategy() {
        return furniturePartPricingStrategy;
    }

    public void setFurniturePartPricingStrategy(String furniturePartPricingStrategy) {
        this.furniturePartPricingStrategy = furniturePartPricingStrategy;
    }

    public Double getFurniturePartValue() {
        return furniturePartValue;
    }

    public void setFurniturePartValue(Double furniturePartValue) {
        this.furniturePartValue = furniturePartValue;
    }

    public List<CustomItemDTO> getCustomItem() {
        return customItem;
    }

    public void setCustomItem(List<CustomItemDTO> customItem) {
        this.customItem = customItem;
    }

    public WorkshopStrategyDTO getWorkshopStrategy() {
        return workshopStrategy;
    }

    public void setWorkshopStrategy(WorkshopStrategyDTO workshopStrategy) {
        this.workshopStrategy = workshopStrategy;
    }

    public ChipPreferenceDTO getChipPreference() {
        return chipPreference;
    }

    public void setChipPreference(ChipPreferenceDTO chipPreference) {
        this.chipPreference = chipPreference;
    }

    @Override
    public String toString() {
        return "ItemValueConfigDTO{" +
                "source='" + source + '\'' +
                ", version='" + version + '\'' +
                ", useActivityAverageStage=" + useActivityAverageStage +
                ", useActivityAverageStageAndUnlimitedItem=" + useActivityAverageStageAndUnlimitedItem +
                ", sampleSize=" + sampleSize +
                ", stageBlacklist=" + stageBlacklist +
                ", stageWhitelist=" + stageWhitelist +
                ", orundumPricingStrategy='" + orundumPricingStrategy + '\'' +
                ", orundumValue=" + orundumValue +
                ", originitePrimePricingStrategy='" + originitePrimePricingStrategy + '\'' +
                ", originitePrimeCoefficient=" + originitePrimeCoefficient +
                ", kernelHeadhuntingPermitPricingStrategy='" + kernelHeadhuntingPermitPricingStrategy + '\'' +
                ", kernelHeadhuntingPermitCoefficient=" + kernelHeadhuntingPermitCoefficient +
                ", lmdPricingStrategy='" + lmdPricingStrategy + '\'' +
                ", lmdCoefficient=" + lmdCoefficient +
                ", expPricingStrategy='" + expPricingStrategy + '\'' +
                ", expCoefficient=" + expCoefficient +
                ", modUnlockTokenPricingStrategy='" + modUnlockTokenPricingStrategy + '\'' +
                ", modUnlockTokenValue=" + modUnlockTokenValue +
                ", recruitmentPermitPricingStrategy='" + recruitmentPermitPricingStrategy + '\'' +
                ", recruitmentPermitValue=" + recruitmentPermitValue +
                ", furniturePartPricingStrategy='" + furniturePartPricingStrategy + '\'' +
                ", furniturePartValue=" + furniturePartValue +
                ", customItemDTO=" + customItem +
                ", workshopStrategyDTO=" + workshopStrategy +
                ", chipPreferenceDTO=" + chipPreference +
                '}';
    }
}




