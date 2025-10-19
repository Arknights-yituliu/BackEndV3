package com.lhs.entity.dto.item.custom;

public class WorkshopItemDTO {

    private String strategy;
    private Double byproductRateIncrease;

    public WorkshopItemDTO() {
    }

    public WorkshopItemDTO(String strategy, Double byproductRateIncrease) {
        this.strategy = strategy;
        this.byproductRateIncrease = byproductRateIncrease;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Double getByproductRateIncrease() {
        return byproductRateIncrease;
    }

    public void setByproductRateIncrease(Double byproductRateIncrease) {
        this.byproductRateIncrease = byproductRateIncrease;
    }
}
