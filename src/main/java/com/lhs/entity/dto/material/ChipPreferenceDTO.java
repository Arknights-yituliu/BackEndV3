package com.lhs.entity.dto.material;

public class ChipPreferenceDTO {
    private String TANK_MEDIC;
    private String SNIPER_CASTER;
    private String PIONEER_SUPPORT;
    private String WARRIOR_SPECIAL;

    public ChipPreferenceDTO() {
    }

    public ChipPreferenceDTO(String TANK_MEDIC, String SNIPER_CASTER, String PIONEER_SUPPORT, String WARRIOR_SPECIAL) {
        this.TANK_MEDIC = TANK_MEDIC;
        this.SNIPER_CASTER = SNIPER_CASTER;
        this.PIONEER_SUPPORT = PIONEER_SUPPORT;
        this.WARRIOR_SPECIAL = WARRIOR_SPECIAL;
    }

    public String getTANK_MEDIC() {
        return TANK_MEDIC;
    }

    public void setTANK_MEDIC(String TANK_MEDIC) {
        this.TANK_MEDIC = TANK_MEDIC;
    }

    public String getSNIPER_CASTER() {
        return SNIPER_CASTER;
    }

    public void setSNIPER_CASTER(String SNIPER_CASTER) {
        this.SNIPER_CASTER = SNIPER_CASTER;
    }

    public String getPIONEER_SUPPORT() {
        return PIONEER_SUPPORT;
    }

    public void setPIONEER_SUPPORT(String PIONEER_SUPPORT) {
        this.PIONEER_SUPPORT = PIONEER_SUPPORT;
    }

    public String getWARRIOR_SPECIAL() {
        return WARRIOR_SPECIAL;
    }

    public void setWARRIOR_SPECIAL(String WARRIOR_SPECIAL) {
        this.WARRIOR_SPECIAL = WARRIOR_SPECIAL;
    }
}
