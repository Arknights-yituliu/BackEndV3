package com.lhs.entity.dto.item.custom;

public class WorkshopItemDTO {

    private String strategy;
    private Object byproductRateIncrease;

    public WorkshopItemDTO() {
    }

    public WorkshopItemDTO(String strategy, Object byproductRateIncrease) {
        this.strategy = strategy;
        this.byproductRateIncrease = byproductRateIncrease;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Object getByproductRateIncrease() {
        return byproductRateIncrease;
    }

    public void setByproductRateIncrease(Object byproductRateIncrease) {
        this.byproductRateIncrease = byproductRateIncrease;
    }
}
