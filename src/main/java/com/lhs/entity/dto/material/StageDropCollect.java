package com.lhs.entity.dto.material;

import java.util.HashMap;
import java.util.Map;

public class StageDropCollect {
    private Integer times = 0;

    private Map<String,Integer> timesMap = new HashMap<>();
    private Map<String,Integer> dropMap = new HashMap<>();

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public Map<String, Integer> getTimesMap() {
        return timesMap;
    }

    public void setTimesMap(Map<String, Integer> timesMap) {
        this.timesMap = timesMap;
    }

    public Map<String, Integer> getDropMap() {
        return dropMap;
    }

    public void setDropMap(Map<String, Integer> dropMap) {
        this.dropMap = dropMap;
    }


    public void addTimes(Integer times){
        this.times += times;
    }


}
