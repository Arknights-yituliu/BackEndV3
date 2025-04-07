package com.lhs.entity.vo.survey;


import java.util.Map;


public class OperatorProgressionStatisticalDataVO {

    private String charId;
    private Integer rarity;
    private Double own;
    private Map<String,Double> elite;
    private Map<String,Double> skill1;
    private Map<String,Double> skill2;
    private Map<String,Double> skill3;
    private Map<String,Double> modX;
    private Map<String,Double> modY;
    private Map<String,Double> modD;
    private Map<String,Double> modA;


    public String getCharId() {
        return charId;
    }

    public void setCharId(String charId) {
        this.charId = charId;
    }

    public Integer getRarity() {
        return rarity;
    }

    public void setRarity(Integer rarity) {
        this.rarity = rarity;
    }

    public Double getOwn() {
        return own;
    }

    public void setOwn(Double own) {
        this.own = own;
    }

    public Map<String, Double> getElite() {
        return elite;
    }

    public void setElite(Map<String, Double> elite) {
        this.elite = elite;
    }

    public Map<String, Double> getSkill1() {
        return skill1;
    }

    public void setSkill1(Map<String, Double> skill1) {
        this.skill1 = skill1;
    }

    public Map<String, Double> getSkill2() {
        return skill2;
    }

    public void setSkill2(Map<String, Double> skill2) {
        this.skill2 = skill2;
    }

    public Map<String, Double> getSkill3() {
        return skill3;
    }

    public void setSkill3(Map<String, Double> skill3) {
        this.skill3 = skill3;
    }

    public Map<String, Double> getModX() {
        return modX;
    }

    public void setModX(Map<String, Double> modX) {
        this.modX = modX;
    }

    public Map<String, Double> getModY() {
        return modY;
    }

    public void setModY(Map<String, Double> modY) {
        this.modY = modY;
    }

    public Map<String, Double> getModD() {
        return modD;
    }

    public void setModD(Map<String, Double> modD) {
        this.modD = modD;
    }

    public Map<String, Double> getModA() {
        return modA;
    }

    public void setModA(Map<String, Double> modA) {
        this.modA = modA;
    }
}
