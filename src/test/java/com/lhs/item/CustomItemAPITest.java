package com.lhs.item;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lhs.BackEndApplication;
import com.lhs.common.util.PenguinDataUtil;
import com.lhs.entity.dto.item.custom.ChipPreferenceDTO;
import com.lhs.entity.dto.item.custom.CustomItemDTO;
import com.lhs.entity.dto.item.custom.ItemInfoDTO;
import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;
import com.lhs.entity.dto.item.custom.StageDropAndInfoDTO;
import com.lhs.entity.dto.item.custom.WorkshopItemDTO;
import com.lhs.entity.dto.item.custom.WorkshopStrategyDTO;
import com.lhs.service.material.CustomItemService;
import com.lhs.service.material.PenguinDataService;

import jakarta.annotation.Resource;

/**
 * CustomItemService 测试类
 */
@SpringBootTest(classes = BackEndApplication.class)
public class CustomItemAPITest {

    @Resource
    private CustomItemService customItemService;
    @Resource
    private PenguinDataService penguinDataServiceService;

    /**
     * 测试 getCustomItemList 方法
     */
    @Test
    void getCustomItem() {
        ItemValueConfigDTO config = buildDefaultConfig();
        List<ItemInfoDTO> itemList = customItemService.getCustomItemList(config);
        for(ItemInfoDTO item : itemList) {
            System.out.println(item.getItemValue());
        }
       
    }


    @Test
    void getStageCollect() {
         ItemValueConfigDTO config = buildDefaultConfig();
         Map<String, List<StageDropAndInfoDTO>> stageDropCollect = penguinDataServiceService.getStageDropCollect(config);
         for(String stage : stageDropCollect.keySet()) {
            System.out.println(stage);
            List<StageDropAndInfoDTO> stageList = stageDropCollect.get(stage);
            for(StageDropAndInfoDTO stageItem : stageList) {
                System.out.println(stageItem);
            }
         }
        
       
    }

    /**
     * 构造与前端默认配置一致的 ItemValueConfigDTO
     */
    private ItemValueConfigDTO buildDefaultConfig() {
        ItemValueConfigDTO config = new ItemValueConfigDTO();
        config.setSource("penguin");
        config.setVersion("v1.0");

        // 定价作战集
        config.setUseActivityAverageStage(false);
        config.setSampleSize(300);
        config.setStageBlacklist(Collections.emptySet());
        config.setStageWhitelist(Collections.emptySet());

        // 自定义其他物品价值
        config.setOrundumPricingStrategy("ORUNDUM_PRICING_ORIGINITE_PRIME");
        config.setOrundumValue(3.0 / 4);

        config.setOriginitePrimePricingStrategy("ORIGINITE_PRIME_PRICING_ORUNDUM");
        config.setOriginitePrimeCoefficient(180.0);

        config.setKernelHeadhuntingPermitPricingStrategy("KERNEL_HEADHUNTING_PERMIT_PRICING_DISTINCTION_CERTIFICATE");
        config.setKernelHeadhuntingPermitCoefficient(216.0 / 258);

        config.setLmdPricingStrategy("LMD_PRICING_CE-6");
        config.setLmdCoefficient(1.0);

        config.setExpPricingStrategy("EXP_PRICING_BASE_LVL_3_TRADING_POST");
        config.setExpCoefficient(145.0 / 229);

        config.setModUnlockTokenPricingStrategy("MOD_UNLOCK_TOKEN_PRICING_PURCHASE_CERTIFICATE");
        config.setModUnlockTokenValue(120.0 * 30 / 21);

        config.setRecruitmentPermitPricingStrategy("RECRUITMENT_PERMIT_PRICING_3_4");
        config.setRecruitmentPermitValue(null);

        config.setFurniturePartPricingStrategy("FURNITURE_PART_PRICING_ZERO");
        config.setFurniturePartValue(0.0);

        // 自定义精英材料价值
        config.setCustomItem(Arrays.asList(
                new CustomItemDTO("30073", 1.8),
                new CustomItemDTO("30083", 2.16),
                new CustomItemDTO("30093", 2.52),
                new CustomItemDTO("30103", 2.88)
        ));

        // 加工站策略
        config.setWorkshopStrategy(new WorkshopStrategyDTO(
                new WorkshopItemDTO("WORKSHOP_STRATEGY_COMMON", 1.0),
                new WorkshopItemDTO("WORKSHOP_STRATEGY_COMMON", 1.0),
                new WorkshopItemDTO("WORKSHOP_STRATEGY_COMMON", 1.0),
                new WorkshopItemDTO("WORKSHOP_STRATEGY_COMMON", 1.0),
                new WorkshopItemDTO("WORKSHOP_STRATEGY_COMMON", 0.8),
                new WorkshopItemDTO("WORKSHOP_STRATEGY_COMMON", 0.8),
                new WorkshopItemDTO("WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN", null),
                new WorkshopItemDTO("WORKSHOP_STRATEGY_COMMON", 0.8),
                new WorkshopItemDTO("WORKSHOP_STRATEGY_COMMON", 0.8)
        ));

        // 芯片加工策略
        config.setChipPreference(new ChipPreferenceDTO("BALANCED", "BALANCED", "BALANCED", "BALANCED"));

        return config;
    }
}
