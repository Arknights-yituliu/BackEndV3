package com.lhs.entity.dto.material;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FloatingValueItem {

    //赤金
    private Double itemValue3003;
    //技能书3
    private Double itemValue3303;
    //技能书2
    private Double itemValue3302;
    //技能书1
    private Double itemValue3301;
    //采购凭证
    private Double itemValue4006;
    //芯片助剂
    private Double itemValue32001;
    //无人机
    private Double itemValueBaseAp;
    //芯片
    private Double chip1Value;
    //芯片组
    private Double chip2Value;
    //双芯片
    private Double chip3Value;
    private Double itemValueLMD;
    private Double itemValueEXP;


    public Map<String, Double> initValue(Double LMDValue, Double EXPValue, Boolean chipIsValueConsistent) {

        //采购凭证  （关卡AP - 龙门币价值*关卡掉落*倍率*关卡AP)/掉落数
        itemValue4006 = (30 - LMDValue * 12 * 30) / 21;
        //芯片助剂
        itemValue32001 = itemValue4006 * 90;
        //无人机  经验书价值 * 经验书产出数量
        itemValueBaseAp = EXPValue * 50 / 3;
        //赤金  无人机/赤金产出数量（1/24)
        itemValue3003 = itemValueBaseAp * 24;
        //技能书3
        itemValue3303 = (30 - LMDValue * 12 * 30) / (2 + 1.5 * (1 + 0.18) / 3 + 1.5 * (1 + 0.18) * (1 + 0.18) / 9);
        //技能书2
        itemValue3302 = 1.18 * itemValue3303 / 3;
        //技能书1
        itemValue3301 = 1.18 * itemValue3302 / 3;

        //芯片2级本 扣除龙门币
        chip2Value = 36 - 36 * LMDValue * 12;
        //芯片1级本 扣除龙门币
        chip1Value = 18 - 18 * LMDValue * 12;

        chip3Value = chip2Value * 2 +  itemValue32001;

        HashMap<String, Double> map = new HashMap<>();

        map.put("3003", itemValue3003);
        map.put("3301", itemValue3301);
        map.put("3302", itemValue3302);
        map.put("3303", itemValue3303);
        map.put("4006", itemValue4006);
        map.put("32001", itemValue32001);
        map.put("base_ap", itemValueBaseAp);
        map.put("LMD", LMDValue);
        map.put("EXP", EXPValue);

        if (chipIsValueConsistent) {
            calculationChipIsValueConsistent(map);
        } else {
            calculationChipIsNotValueConsistent(map);
        }

        return map;
    }


    private void calculationChipIsValueConsistent(Map<String, Double> map) {

        map.put("3211", chip1Value);
        map.put("3221", chip1Value);
        map.put("3231", chip1Value);
        map.put("3241", chip1Value);
        map.put("3251", chip1Value);
        map.put("3261", chip1Value);
        map.put("3271", chip1Value);
        map.put("3281", chip1Value);

        map.put("3212", chip2Value);
        map.put("3222", chip2Value);
        map.put("3232", chip2Value);
        map.put("3242", chip2Value);
        map.put("3252", chip2Value);
        map.put("3262", chip2Value);
        map.put("3272", chip2Value);
        map.put("3282", chip2Value);


        map.put("3213", chip3Value);
        map.put("3223", chip3Value);
        map.put("3233", chip3Value);
        map.put("3243", chip3Value);
        map.put("3253", chip3Value);
        map.put("3263", chip3Value);
        map.put("3273", chip3Value);
        map.put("3283", chip3Value);

    }


    private void calculationChipIsNotValueConsistent(Map<String, Double> map) {

        double CHIP_BY_PRODUCT_YIELD = 0.18;
        //高价值芯片
        double CHIP1_HEIGHT_VALUE = (6 - CHIP_BY_PRODUCT_YIELD) / 5 * chip1Value;
        //低价值芯片
        double CHIP1_LOW_VALUE = (4 + CHIP_BY_PRODUCT_YIELD) / 5 * chip1Value;
        //高价值芯片组
        double CHIP2_HEIGHT_VALUE = (6 - CHIP_BY_PRODUCT_YIELD) / 5 * chip2Value;
        //低价值芯片组
        double CHIP2_LOW_VALUE = (4 + CHIP_BY_PRODUCT_YIELD) / 5 * chip2Value;
        //高价值双芯片
        double CHIP3_HEIGHT_VALUE = CHIP2_HEIGHT_VALUE * 2  + itemValue32001;
        //低价值双芯片
        double CHIP3_LOW_VALUE = CHIP2_LOW_VALUE * 2  + itemValue32001;

        map.put("3231", CHIP1_HEIGHT_VALUE);
        map.put("3221", CHIP1_HEIGHT_VALUE);
        map.put("3261", CHIP1_LOW_VALUE);
        map.put("3281", CHIP1_LOW_VALUE);
        map.put("3271", chip1Value);
        map.put("3211", chip1Value);
        map.put("3241", chip1Value);
        map.put("3251", chip1Value);

        map.put("3232", CHIP2_HEIGHT_VALUE);
        map.put("3222", CHIP2_HEIGHT_VALUE);
        map.put("3262", CHIP2_LOW_VALUE);
        map.put("3282", CHIP2_LOW_VALUE);
        map.put("3272", chip2Value);
        map.put("3212", chip2Value);
        map.put("3242", chip2Value);
        map.put("3252", chip2Value);

        map.put("3233", CHIP3_HEIGHT_VALUE);
        map.put("3223", CHIP3_HEIGHT_VALUE);
        map.put("3263", CHIP3_LOW_VALUE);
        map.put("3283", CHIP3_LOW_VALUE);
        map.put("3273", chip3Value);
        map.put("3213", chip3Value);
        map.put("3243", chip3Value);
        map.put("3253", chip3Value);
    }

}
