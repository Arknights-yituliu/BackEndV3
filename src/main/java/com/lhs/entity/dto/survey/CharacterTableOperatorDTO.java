package com.lhs.entity.dto.survey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 干员角色表精简数据（对应 character_table_simple.v2.json 中单个干员）
 *
 * <p>源文件字段极多，此处只保留 open-api 干员数据接口用到的技能与模组信息，
 * 配合 ignoreUnknown 忽略其余字段，既减小缓存体积又避免解析报错。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterTableOperatorDTO {

    /** 干员技能列表 */
    private List<Skill> skills;

    /** 干员模组列表 */
    private List<Equip> equip;

    public List<Skill> getSkills() {
        return skills;
    }

    public void setSkills(List<Skill> skills) {
        this.skills = skills;
    }

    public List<Equip> getEquip() {
        return equip;
    }

    public void setEquip(List<Equip> equip) {
        this.equip = equip;
    }

    /**
     * 技能信息（仅需技能ID）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Skill {

        /** 技能ID */
        private String skillId;

        public String getSkillId() {
            return skillId;
        }

        public void setSkillId(String skillId) {
            this.skillId = skillId;
        }
    }

    /**
     * 模组信息（模组ID + 模组类型）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Equip {

        /** 模组ID */
        private String uniEquipId;

        /** 模组类型（X/Y/D/A/B） */
        private String typeName2;

        public String getUniEquipId() {
            return uniEquipId;
        }

        public void setUniEquipId(String uniEquipId) {
            this.uniEquipId = uniEquipId;
        }

        public String getTypeName2() {
            return typeName2;
        }

        public void setTypeName2(String typeName2) {
            this.typeName2 = typeName2;
        }
    }
}
