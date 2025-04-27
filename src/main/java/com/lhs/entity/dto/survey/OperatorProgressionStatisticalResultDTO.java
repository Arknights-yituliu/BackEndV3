package com.lhs.entity.dto.survey;

import java.util.HashMap;
import java.util.Map;

public class OperatorProgressionStatisticalResultDTO {
    private String charId;
    private Integer own;
    private Integer sampleSize;
    private Map<Integer, Integer> elite;
    private Map<Integer, Integer> skill1;
    private Map<Integer, Integer> skill2;
    private Map<Integer, Integer> skill3;
    private Map<Integer, Integer> modA;
    private Map<Integer, Integer> modX;
    private Map<Integer, Integer> modY;
    private Map<Integer, Integer> modD;

    {
        sampleSize = 0;
        own = 0;
        elite = new HashMap<>();
        skill1 = new HashMap<>();
        skill2 = new HashMap<>();
        skill3 = new HashMap<>();
        modA = new HashMap<>();
        modX = new HashMap<>();
        modY = new HashMap<>();
        modD = new HashMap<>();
    }

    public String getCharId() {
        return charId;
    }
    public void setCharId(String charId) {
        this.charId = charId;
    }

    public Integer getOwn() {
        return own;
    }

    public void setOwn(Integer own) {
        this.own = own;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Map<Integer, Integer> getElite() {
        return elite;
    }

    public void setElite(Map<Integer, Integer> elite) {
        this.elite = elite;
    }

    public Map<Integer, Integer> getSkill1() {
        return skill1;
    }

    public void setSkill1(Map<Integer, Integer> skill1) {
        this.skill1 = skill1;
    }

    public Map<Integer, Integer> getSkill2() {
        return skill2;
    }

    public void setSkill2(Map<Integer, Integer> skill2) {
        this.skill2 = skill2;
    }

    public Map<Integer, Integer> getSkill3() {
        return skill3;
    }

    public void setSkill3(Map<Integer, Integer> skill3) {
        this.skill3 = skill3;
    }

    public Map<Integer, Integer> getModA() {
        return modA;
    }

    public void setModA(Map<Integer, Integer> modA) {
        this.modA = modA;
    }

    public Map<Integer, Integer> getModX() {
        return modX;
    }

    public void setModX(Map<Integer, Integer> modX) {
        this.modX = modX;
    }

    public Map<Integer, Integer> getModY() {
        return modY;
    }

    public void setModY(Map<Integer, Integer> modY) {
        this.modY = modY;
    }

    public Map<Integer, Integer> getModD() {
        return modD;
    }

    public void setModD(Map<Integer, Integer> modD) {
        this.modD = modD;
    }

    public void increaseOwn() {
        this.own++;
    }
    public void increaseSampleSize() {
        this.sampleSize++;
    }

    public void mergeElite(Integer elite) {

        this.elite.merge(elite, 1, Integer::sum);
    }

    public void mergeSkill1(Integer skill1) {

        this.skill1.merge(skill1, 1, Integer::sum);
    }

    public void mergeSkill2(Integer skill2) {
        this.skill2.merge(skill2, 1, Integer::sum);
    }

    public void mergeSkill3(Integer skill3) {
        this.skill3.merge(skill3, 1, Integer::sum);
    }

    public void mergeModX(Integer modX) {
        this.modX.merge(modX, 1, Integer::sum);
    }

    public void mergeModY(Integer modY) {
        this.modY.merge(modY, 1, Integer::sum);
    }

    public void mergeModD(Integer modD) {
        this.modD.merge(modD, 1, Integer::sum);
    }

    public void mergeModA(Integer modA) {
        this.modA.merge(modA, 1, Integer::sum);
    }


}
