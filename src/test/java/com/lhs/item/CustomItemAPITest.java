package com.lhs.item;

import java.util.*;

import com.aliyun.oss.common.utils.HttpUtil;
import com.lhs.common.util.HttpRequestUtil;
import com.lhs.common.util.JsonMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import com.lhs.BackEndApplication;
import com.lhs.entity.dto.material.ChipPreferenceDTO;
import com.lhs.entity.dto.material.CustomItemDTO;
import com.lhs.entity.dto.material.ItemInfoDTO;
import com.lhs.entity.dto.material.ItemValueConfigDTO;
import com.lhs.entity.dto.material.StageDropAndInfoDTO;
import com.lhs.entity.dto.material.WorkshopItemDTO;
import com.lhs.entity.dto.material.WorkshopStrategyDTO;
import com.lhs.service.material.CustomItemService;
import com.lhs.service.material.PenguinDataService;
import com.lhs.service.material.impl.CustomItemServiceImpl;

import jakarta.annotation.Resource;

/**
 * CustomItemService 测试类
 */
@SpringBootTest(classes = BackEndApplication.class)
public class CustomItemAPITest {

    @Resource
    private CustomItemService customItemService;
    @Resource
    private CustomItemServiceImpl customItemServiceImpl;
    @Resource
    private PenguinDataService penguinDataServiceService;

    /**
     * 测试 getCustomItemList 方法
     */
    @Test
    void getCustomItem() {
//        ItemValueConfigDTO config = buildDefaultConfig();
        ItemValueConfigDTO config = new ItemValueConfigDTO();
        List<ItemInfoDTO> itemList = customItemService.getCustomItemList(config);
        for(ItemInfoDTO item : itemList) {
            System.out.println(item.getItemName()+"  "+ item.getItemValue());
        }
       
    }

    @Test
    void API(){
        String post = HttpRequestUtil.post("https://backend.yituliu.cn/item/v7/value", new HashMap<>(), JsonMapper.toJSONString(new ItemValueConfigDTO()));
        System.out.println(post);
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
     * 测试 fillDefaultConfig 默认值填充逻辑
     */
    @Test
    void testFillDefaultConfig() throws Exception {
        ItemValueConfigDTO config = new ItemValueConfigDTO();

        ReflectionTestUtils.invokeMethod(customItemServiceImpl, "fillDefaultConfig", config);

        assertEquals("penguin", config.getSource());
        assertEquals("v1.0", config.getVersion());
        assertEquals(false, config.getUseActivityAverageStage());
        assertEquals(300, config.getSampleSize());
        assertEquals(Collections.emptySet(), config.getStageBlacklist());
        assertEquals(Collections.emptySet(), config.getStageWhitelist());
        assertEquals("ORUNDUM_PRICING_ORIGINITE_PRIME", config.getOrundumPricingStrategy());
        assertEquals(3.0 / 4, config.getOrundumValue());
        assertEquals("ORIGINITE_PRIME_PRICING_ORUNDUM", config.getOriginitePrimePricingStrategy());
        assertEquals(180.0, config.getOriginitePrimeCoefficient());
        assertEquals("KERNEL_HEADHUNTING_PERMIT_PRICING_DISTINCTION_CERTIFICATE",
                config.getKernelHeadhuntingPermitPricingStrategy());
        assertEquals(216.0 / 258, config.getKernelHeadhuntingPermitCoefficient());
        assertEquals("LMD_PRICING_CE-6", config.getLmdPricingStrategy());
        assertEquals(1.0, config.getLmdCoefficient());
        assertEquals("EXP_PRICING_BASE_LVL_3_TRADING_POST", config.getExpPricingStrategy());
        assertEquals(145.0 / 229, config.getExpCoefficient());
        assertEquals("MOD_UNLOCK_TOKEN_PRICING_PURCHASE_CERTIFICATE",
                config.getModUnlockTokenPricingStrategy());
        assertEquals(120.0 * 30 / 21, config.getModUnlockTokenValue());
        assertEquals("RECRUITMENT_PERMIT_PRICING_3_4",
                config.getRecruitmentPermitPricingStrategy());
        assertNull(config.getRecruitmentPermitValue());
        assertEquals("FURNITURE_PART_PRICING_ZERO",
                config.getFurniturePartPricingStrategy());
        assertEquals(0.0, config.getFurniturePartValue());
        assertNotNull(config.getCustomItem());
        assertEquals(4, config.getCustomItem().size());
        assertNotNull(config.getWorkshopStrategy());
        assertNotNull(config.getChipPreference());
        assertEquals("BALANCED", config.getChipPreference().getTANK_MEDIC());
        assertEquals("BALANCED", config.getChipPreference().getSNIPER_CASTER());
        assertEquals("BALANCED", config.getChipPreference().getPIONEER_SUPPORT());
        assertEquals("BALANCED", config.getChipPreference().getWARRIOR_SPECIAL());
    }

    /**
     * 测试 fillDefaultConfig 不覆盖已有值
     */
    @Test
    void testFillDefaultConfigDoesNotOverwrite() throws Exception {
        ItemValueConfigDTO config = new ItemValueConfigDTO();
        config.setSource("custom");
        config.setSampleSize(500);

        ReflectionTestUtils.invokeMethod(customItemServiceImpl, "fillDefaultConfig", config);

        assertEquals("custom", config.getSource());
        assertEquals(500, config.getSampleSize());
        assertEquals("v1.0", config.getVersion());
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
