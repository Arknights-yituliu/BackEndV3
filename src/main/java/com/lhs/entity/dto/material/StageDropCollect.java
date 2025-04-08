package com.lhs.entity.dto.material;

import java.util.HashMap;
import java.util.Map;

public class StageDropCollect {
    private Integer times = 0;
    private Map<String,Integer> drops = new HashMap<>();

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public Map<String, Integer> getDrops() {
        return drops;
    }

    public void setDrops(Map<String, Integer> drops) {
        this.drops = drops;
    }


    public void addTimes(Integer times){
        this.times += times;
    }
}
