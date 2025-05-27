package com.lhs.entity.dto.rogue;

import java.util.Date;

public class UserActionOnSeedDTO {
    private Long seedId;
    private String action;

    public Long getSeedId() {
        return seedId;
    }

    public void setSeedId(Long seedId) {
        this.seedId = seedId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
