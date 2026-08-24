package com.lhs.entity.dto.survey;

import java.util.List;

/**
 * 干员练度数据返回DTO（含角色表富化后的数据）
 */
public class OperatorProgressionDataV2DTO {

    /**
     * 干员ID，对应原charId
     */
    private String id;

    /**
     * 干员等级
     */
    private Integer level;

    /**
     * 精英化阶段
     */
    private Integer evolvePhase;

    /**
     * 当前携带技能等级
     */
    private Integer mainSkillLevel;

    /**
     * 技能列表（id + 练度等级）
     */
    private List<SkillInfo> skills;

    /**
     * 模组列表（模组id + 类型 + 等级）
     */
    private List<EquipInfo> equips;

    /**
     * 潜能等级
     */
    private Integer potentialRank;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getEvolvePhase() {
        return evolvePhase;
    }

    public void setEvolvePhase(Integer evolvePhase) {
        this.evolvePhase = evolvePhase;
    }

    public Integer getMainSkillLevel() {
        return mainSkillLevel;
    }

    public void setMainSkillLevel(Integer mainSkillLevel) {
        this.mainSkillLevel = mainSkillLevel;
    }

    public List<SkillInfo> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillInfo> skills) {
        this.skills = skills;
    }

    public List<EquipInfo> getEquips() {
        return equips;
    }

    public void setEquips(List<EquipInfo> equips) {
        this.equips = equips;
    }

    public Integer getPotentialRank() {
        return potentialRank;
    }

    public void setPotentialRank(Integer potentialRank) {
        this.potentialRank = potentialRank;
    }

    /**
     * 技能信息（技能ID + 专精等级）
     */
    public static class SkillInfo {
        private String id;
        private Integer level;

        public SkillInfo() {}

        public SkillInfo(String id, Integer level) {
            this.id = id;
            this.level = level;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }
    }

    /**
     * 模组信息（模组ID + 类型 + 等级）
     */
    public static class EquipInfo {
        private String id;
        private String type;
        private Integer level;

        public EquipInfo() {}

        public EquipInfo(String id, String type, Integer level) {
            this.id = id;
            this.type = type;
            this.level = level;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }
    }
}
