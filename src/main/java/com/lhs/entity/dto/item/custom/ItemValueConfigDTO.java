package com.lhs.entity.dto.item.custom;

import java.util.List;
import java.util.Set;

public class ItemValueConfigDTO {
    private String source;
    private String version;
    private Boolean useActivityAverageStage;
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
    private Object modUnlockTokenValue;
    private String recruitmentPermitPricingStrategy;
    private Object recruitmentPermitValue;
    private String expeditedPlanPricingStrategy;
    private Integer expeditedPlanValue;
    private String furniturePartPricingStrategy;
    private Integer furniturePartValue;
    private List<CustomItemDTO> customItemDTO;
    private WorkshopStrategyDTO workshopStrategyDTO;
    private ChipPreferenceDTO chipPreferenceDTO;

    public ItemValueConfigDTO() {
    }

    public ItemValueConfigDTO(String source, String version, Boolean useActivityAverageStage, Integer sampleSize, Set<String> stageBlacklist, Set<String> stageWhitelist, String orundumPricingStrategy, Double orundumValue, String originitePrimePricingStrategy, Double originitePrimeCoefficient, String kernelHeadhuntingPermitPricingStrategy, Double kernelHeadhuntingPermitCoefficient, String lmdPricingStrategy, Double lmdCoefficient, String expPricingStrategy, Double expCoefficient, String modUnlockTokenPricingStrategy, Object modUnlockTokenValue, String recruitmentPermitPricingStrategy, Object recruitmentPermitValue, String expeditedPlanPricingStrategy, Integer expeditedPlanValue, String furniturePartPricingStrategy, Integer furniturePartValue, List<CustomItemDTO> customItemDTO, WorkshopStrategyDTO workshopStrategyDTO, ChipPreferenceDTO chipPreferenceDTO) {
        this.source = source;
        this.version = version;
        this.useActivityAverageStage = useActivityAverageStage;
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
        this.expeditedPlanPricingStrategy = expeditedPlanPricingStrategy;
        this.expeditedPlanValue = expeditedPlanValue;
        this.furniturePartPricingStrategy = furniturePartPricingStrategy;
        this.furniturePartValue = furniturePartValue;
        this.customItemDTO = customItemDTO;
        this.workshopStrategyDTO = workshopStrategyDTO;
        this.chipPreferenceDTO = chipPreferenceDTO;
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

    public Object getModUnlockTokenValue() {
        return modUnlockTokenValue;
    }

    public void setModUnlockTokenValue(Object modUnlockTokenValue) {
        this.modUnlockTokenValue = modUnlockTokenValue;
    }

    public String getRecruitmentPermitPricingStrategy() {
        return recruitmentPermitPricingStrategy;
    }

    public void setRecruitmentPermitPricingStrategy(String recruitmentPermitPricingStrategy) {
        this.recruitmentPermitPricingStrategy = recruitmentPermitPricingStrategy;
    }

    public Object getRecruitmentPermitValue() {
        return recruitmentPermitValue;
    }

    public void setRecruitmentPermitValue(Object recruitmentPermitValue) {
        this.recruitmentPermitValue = recruitmentPermitValue;
    }

    public String getExpeditedPlanPricingStrategy() {
        return expeditedPlanPricingStrategy;
    }

    public void setExpeditedPlanPricingStrategy(String expeditedPlanPricingStrategy) {
        this.expeditedPlanPricingStrategy = expeditedPlanPricingStrategy;
    }

    public Integer getExpeditedPlanValue() {
        return expeditedPlanValue;
    }

    public void setExpeditedPlanValue(Integer expeditedPlanValue) {
        this.expeditedPlanValue = expeditedPlanValue;
    }

    public String getFurniturePartPricingStrategy() {
        return furniturePartPricingStrategy;
    }

    public void setFurniturePartPricingStrategy(String furniturePartPricingStrategy) {
        this.furniturePartPricingStrategy = furniturePartPricingStrategy;
    }

    public Integer getFurniturePartValue() {
        return furniturePartValue;
    }

    public void setFurniturePartValue(Integer furniturePartValue) {
        this.furniturePartValue = furniturePartValue;
    }

    public List<CustomItemDTO> getCustomItemDTO() {
        return customItemDTO;
    }

    public void setCustomItemDTO(List<CustomItemDTO> customItemDTO) {
        this.customItemDTO = customItemDTO;
    }

    public WorkshopStrategyDTO getWorkshopStrategyDTO() {
        return workshopStrategyDTO;
    }

    public void setWorkshopStrategyDTO(WorkshopStrategyDTO workshopStrategyDTO) {
        this.workshopStrategyDTO = workshopStrategyDTO;
    }

    public ChipPreferenceDTO getChipPreferenceDTO() {
        return chipPreferenceDTO;
    }

    public void setChipPreferenceDTO(ChipPreferenceDTO chipPreferenceDTO) {
        this.chipPreferenceDTO = chipPreferenceDTO;
    }

    @Override
    public String toString() {
        return "ItemValueConfigDTO{" +
                "source='" + source + '\'' +
                ", version='" + version + '\'' +
                ", useActivityAverageStage=" + useActivityAverageStage +
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
                ", expeditedPlanPricingStrategy='" + expeditedPlanPricingStrategy + '\'' +
                ", expeditedPlanValue=" + expeditedPlanValue +
                ", furniturePartPricingStrategy='" + furniturePartPricingStrategy + '\'' +
                ", furniturePartValue=" + furniturePartValue +
                ", customItemDTO=" + customItemDTO +
                ", workshopStrategyDTO=" + workshopStrategyDTO +
                ", chipPreferenceDTO=" + chipPreferenceDTO +
                '}';
    }
}




