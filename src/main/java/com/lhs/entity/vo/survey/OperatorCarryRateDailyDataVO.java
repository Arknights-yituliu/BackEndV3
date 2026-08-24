package com.lhs.entity.vo.survey;

import java.util.List;

public class OperatorCarryRateDailyDataVO {
    private String charId;

    private List<CarryRateDailyDataVO> list;

    private List<Long>  date;

    public String getCharId() {
        return charId;
    }

    public void setCharId(String charId) {
        this.charId = charId;
    }

    public List<CarryRateDailyDataVO> getList() {
        return list;
    }

    public void setList(List<CarryRateDailyDataVO> list) {
        this.list = list;
    }

    public List<Long> getDate() {
        return date;
    }

    public void setDate(List<Long> date) {
        this.date = date;
    }
}
