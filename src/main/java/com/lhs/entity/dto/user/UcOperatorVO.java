package com.lhs.entity.dto.user;

/**
 * UC 单条干员数据（GET /oauth2/ak-accounts/operators 响应中的 items 元素）
 *
 * <p>字段与 UC 侧 OperatorVO 一一对应，命名保持 camelCase 与 UC 响应一致，
 * 数值字段 UC 侧保证不返回 null。</p>
 *
 * @author BackEndV3
 */
public class UcOperatorVO {

    /** 稳定干员编码，对应本地 charId */
    private String id;

    /** 干员星级，0 表示未提供 */
    private Integer rarity;

    /** 干员等级 */
    private Integer level;

    /** 精英化阶段 */
    private Integer evolvePhase;

    /** 基础技能等级 */
    private Integer mainSkillLevel;

    /** 技能 1 等级或状态 */
    private Integer skill1;

    /** 技能 2 等级或状态 */
    private Integer skill2;

    /** 技能 3 等级或状态 */
    private Integer skill3;

    /** X 模组数值 */
    private Integer equipX;

    /** Y 模组数值 */
    private Integer equipY;

    /** D 模组数值 */
    private Integer equipD;

    /** A 模组数值 */
    private Integer equipA;

    /** B 模组数值 */
    private Integer equipB;

    /** 潜能等级，不是星级 */
    private Integer potentialRank;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getRarity() {
        return rarity;
    }

    public void setRarity(Integer rarity) {
        this.rarity = rarity;
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

    public Integer getSkill1() {
        return skill1;
    }

    public void setSkill1(Integer skill1) {
        this.skill1 = skill1;
    }

    public Integer getSkill2() {
        return skill2;
    }

    public void setSkill2(Integer skill2) {
        this.skill2 = skill2;
    }

    public Integer getSkill3() {
        return skill3;
    }

    public void setSkill3(Integer skill3) {
        this.skill3 = skill3;
    }

    public Integer getEquipX() {
        return equipX;
    }

    public void setEquipX(Integer equipX) {
        this.equipX = equipX;
    }

    public Integer getEquipY() {
        return equipY;
    }

    public void setEquipY(Integer equipY) {
        this.equipY = equipY;
    }

    public Integer getEquipD() {
        return equipD;
    }

    public void setEquipD(Integer equipD) {
        this.equipD = equipD;
    }

    public Integer getEquipA() {
        return equipA;
    }

    public void setEquipA(Integer equipA) {
        this.equipA = equipA;
    }

    public Integer getEquipB() {
        return equipB;
    }

    public void setEquipB(Integer equipB) {
        this.equipB = equipB;
    }

    public Integer getPotentialRank() {
        return potentialRank;
    }

    public void setPotentialRank(Integer potentialRank) {
        this.potentialRank = potentialRank;
    }
}
