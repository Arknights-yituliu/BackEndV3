package com.lhs.entity.dto.material;

public class WorkshopStrategyDTO {
    private WorkshopItemDTO eliteMaterialT1toT2;
    private WorkshopItemDTO eliteMaterialT2toT3;
    private WorkshopItemDTO eliteMaterialT3toT4;
    private WorkshopItemDTO eliteMaterialT4toT5;
    private WorkshopItemDTO skillSummary1to2;
    private WorkshopItemDTO skillSummary2to3;
    private WorkshopItemDTO baseMaterial;
    private WorkshopItemDTO chip;
    private WorkshopItemDTO chipPack;

    public WorkshopStrategyDTO() {
    }

    public WorkshopStrategyDTO(WorkshopItemDTO eliteMaterialT1toT2, WorkshopItemDTO eliteMaterialT2toT3, WorkshopItemDTO eliteMaterialT3toT4, WorkshopItemDTO eliteMaterialT4toT5, WorkshopItemDTO skillSummary1to2, WorkshopItemDTO skillSummary2to3, WorkshopItemDTO baseMaterial, WorkshopItemDTO chip, WorkshopItemDTO chipPack) {
        this.eliteMaterialT1toT2 = eliteMaterialT1toT2;
        this.eliteMaterialT2toT3 = eliteMaterialT2toT3;
        this.eliteMaterialT3toT4 = eliteMaterialT3toT4;
        this.eliteMaterialT4toT5 = eliteMaterialT4toT5;
        this.skillSummary1to2 = skillSummary1to2;
        this.skillSummary2to3 = skillSummary2to3;
        this.baseMaterial = baseMaterial;
        this.chip = chip;
        this.chipPack = chipPack;
    }

    public WorkshopItemDTO getEliteMaterialT1toT2() {
        return eliteMaterialT1toT2;
    }

    public void setEliteMaterialT1toT2(WorkshopItemDTO eliteMaterialT1toT2) {
        this.eliteMaterialT1toT2 = eliteMaterialT1toT2;
    }

    public WorkshopItemDTO getEliteMaterialT2toT3() {
        return eliteMaterialT2toT3;
    }

    public void setEliteMaterialT2toT3(WorkshopItemDTO eliteMaterialT2toT3) {
        this.eliteMaterialT2toT3 = eliteMaterialT2toT3;
    }

    public WorkshopItemDTO getEliteMaterialT3toT4() {
        return eliteMaterialT3toT4;
    }

    public void setEliteMaterialT3toT4(WorkshopItemDTO eliteMaterialT3toT4) {
        this.eliteMaterialT3toT4 = eliteMaterialT3toT4;
    }

    public WorkshopItemDTO getEliteMaterialT4toT5() {
        return eliteMaterialT4toT5;
    }

    public void setEliteMaterialT4toT5(WorkshopItemDTO eliteMaterialT4toT5) {
        this.eliteMaterialT4toT5 = eliteMaterialT4toT5;
    }

    public WorkshopItemDTO getSkillSummary1to2() {
        return skillSummary1to2;
    }

    public void setSkillSummary1to2(WorkshopItemDTO skillSummary1to2) {
        this.skillSummary1to2 = skillSummary1to2;
    }

    public WorkshopItemDTO getSkillSummary2to3() {
        return skillSummary2to3;
    }

    public void setSkillSummary2to3(WorkshopItemDTO skillSummary2to3) {
        this.skillSummary2to3 = skillSummary2to3;
    }

    public WorkshopItemDTO getBaseMaterial() {
        return baseMaterial;
    }

    public void setBaseMaterial(WorkshopItemDTO baseMaterial) {
        this.baseMaterial = baseMaterial;
    }

    public WorkshopItemDTO getChip() {
        return chip;
    }

    public void setChip(WorkshopItemDTO chip) {
        this.chip = chip;
    }

    public WorkshopItemDTO getChipPack() {
        return chipPack;
    }

    public void setChipPack(WorkshopItemDTO chipPack) {
        this.chipPack = chipPack;
    }
}
