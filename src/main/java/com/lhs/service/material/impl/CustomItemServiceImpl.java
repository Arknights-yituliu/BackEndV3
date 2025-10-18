package com.lhs.service.material.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.annotation.RedisCacheable;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.item.custom.ItemInfoDTO;
import com.lhs.entity.dto.item.custom.ItemValueConfigDTO;
import com.lhs.entity.dto.item.custom.StageDropDTO;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.po.material.Stage;
import com.lhs.service.material.CustomItemService;
import com.lhs.service.material.StageService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomItemServiceImpl implements CustomItemService {

    private final StageService stageService;

    private static final Integer BASE_LMD_VALUE = 36 / 10000;
    private static final Integer BASE_EXP_VALUE = 36 / 10000;

    public CustomItemServiceImpl(StageService stageService) {
        this.stageService = stageService;
    }

    @Override
    public void customItemValueCalculation() {

    }


    @Override
    public void getCustomItemList(ItemValueConfigDTO itemValueConfigDTO) {
        checkItemValueConfig(itemValueConfigDTO);

        JsonNode recruitmentTableJson = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "recruitment_table.json"));

        //招募许可定价方案
        JsonNode recruitmentPermitPricing = recruitmentTableJson.get("recruitmentPermitPricing");
        //公开招募干员概率
        JsonNode operatorRecruitmentRates = recruitmentTableJson.get("operatorRecruitmentRates");

        String itemInfoText = FileUtil.read(ConfigUtil.DataFilePath + "item_info.json");

        List<ItemInfoDTO> itemInfoDTOList = JsonMapper.parseJSONArray(itemInfoText, new TypeReference<>() {
        });



        //将精英材料根据品质进行分类，方便后面计算每级品质精英材料的加工站期望产出
        Map<Integer, Map<String, Double>> workshopByproductWeightMap = new HashMap<>();


        //循环所有精英材料
        for (ItemInfoDTO itemInfoDTO : itemInfoDTOList) {
            //权重值
            double weight = itemInfoDTO.getWeight();
            //权重为0跳过
            if (weight <= 0) {
                continue;
            }
            //分组id10600以上为非精英材料，此时结束循环
            if (itemInfoDTO.getGroupId() > 10600) {
                break;
            }
            //稀有度
            int rarity = itemInfoDTO.getRarity();
            //物品ID
            String itemId = itemInfoDTO.getItemId();
            //将物品权重根据稀有度进行分类存入map
            workshopByproductWeightMap.computeIfAbsent(rarity, k -> new HashMap<>()).put(itemId, weight);
        }

    }

    @RedisCacheable(key = "Json:Penguin_Matrix")
    @Override
    public void getStageDropCollect(ItemValueConfigDTO itemValueConfigDTO) {
        String penguinMatrixText = FileUtil.read(ConfigUtil.Penguin + "auto.json");
        String matrixText = JsonMapper.parseJSONObject(penguinMatrixText).get("matrix").toPrettyString();
        List<PenguinMatrixDTO> penguinMatrixDTOList = JsonMapper.parseJSONArray(matrixText, new TypeReference<>() {
        });

        String ytlStageDataText = FileUtil.read(ConfigUtil.DataFilePath + "ytl_stage_info.json");
        JsonNode jsonNodeObj = JsonMapper.parseJSONObject(ytlStageDataText);

        Map<String, Stage> stageInfoMap = stageService.getStageInfoMap();


        int sampleSize = itemValueConfigDTO.getSampleSize();

        Set<String> stageBlcklistSet = new HashSet<>();
        if(itemValueConfigDTO.getStageBlacklist()!=null){
            stageBlcklistSet = itemValueConfigDTO.getStageBlacklist();
        }

        Map<String, PenguinMatrixDTO> toughStageMap = penguinMatrixDTOList.stream()
                .filter(item -> item.getStageId().contains("tough"))
                .collect(Collectors
                        .toMap(item -> item.getStageId().replace("tough","main")+"—"+item.getItemId(), item -> item));

        Map<String,List<StageDropDTO>> stageDropCollect = new HashMap<>();

        for(PenguinMatrixDTO penguinMatrixDTO:penguinMatrixDTOList){

            String stageId = penguinMatrixDTO.getStageId();
            String itemId = penguinMatrixDTO.getItemId();
            Integer quantity = penguinMatrixDTO.getQuantity();
            Integer times = penguinMatrixDTO.getTimes();
            Long start = penguinMatrixDTO.getStart();
            Long end = penguinMatrixDTO.getEnd();

            if(stageBlcklistSet.contains(stageId)){
                continue;
            }

            if((stageId.contains("main_14")&&end!=null)||stageId.contains("tough")){
                continue;
            }

            if(times<sampleSize){
                continue;
            }

            Stage stage = stageInfoMap.get(stageId);

            if(stage==null){
                continue;
            }

            String main14StageDropMergeKey = stageId+"—"+itemId;



        }


    }

    private void checkItemValueConfig(ItemValueConfigDTO configDTO) {
        checkNotNull(configDTO.getUseActivityAverageStage());
        checkNotNull(configDTO.getSampleSize());
        checkNotNull(configDTO.getStageBlacklist());
        checkNotNull(configDTO.getStageWhitelist());
        checkNotNull(configDTO.getKernelHeadhuntingPermitPricingStrategy());
        checkNotNull(configDTO.getKernelHeadhuntingPermitCoefficient());
        checkNotNull(configDTO.getLmdPricingStrategy());
        checkNotNull(configDTO.getLmdCoefficient());
        checkNotNull(configDTO.getExpPricingStrategy());
        checkNotNull(configDTO.getExpCoefficient());
        checkNotNull(configDTO.getModUnlockTokenPricingStrategy());
        checkNotNull(configDTO.getModUnlockTokenValue());
        checkNotNull(configDTO.getRecruitmentPermitPricingStrategy());
        checkNotNull(configDTO.getRecruitmentPermitValue());
        checkNotNull(configDTO.getExpeditedPlanPricingStrategy());
        checkNotNull(configDTO.getExpeditedPlanValue());
        checkNotNull(configDTO.getFurniturePartPricingStrategy());
        checkNotNull(configDTO.getFurniturePartValue());
        checkNotNull(configDTO.getCustomItemDTO());
        checkNotNull(configDTO.getWorkshopStrategyDTO());
        checkNotNull(configDTO.getChipPreferenceDTO());

        if(configDTO.getOrundumPricingStrategy()==null&&configDTO.getOrundumValue()==null){
            configDTO.setOrundumValue(0.75);
        }

        if(configDTO.getOrundumValue()<=0){
            throw new ServiceException(ResultCode.ORUNDUM_VALUE_CANNOT_BE_LESS_THAN_0);
        }

        if(configDTO.getOriginitePrimePricingStrategy()==null&&configDTO.getOriginitePrimeCoefficient()==null){
            configDTO.setOrundumValue(180.0);
        }

        if(configDTO.getOriginitePrimeCoefficient()<=0){
            throw new ServiceException(ResultCode.ORIGINITE_PRIME_VALUE_CANNOT_BE_LESS_THAN_0);
        }





        if (configDTO.getSampleSize() <= 50) {
            throw new ServiceException(ResultCode.SAMPLE_SIZE_CANNOT_BE_ZERO_OR_LESS_THAN_50);
        }
    }


    private static void checkNotNull(Object value) {
        if (value == null) {
            throw new ServiceException(ResultCode.PARAM_IS_BLANK);
        }
    }


    @RedisCacheable(key = "Json:Recruitment_Table")
    private JsonNode getRecruitmentTable() {
        return JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "recruitment_table.json"));
    }

    @RedisCacheable(key = "Json:Item_Info")
    private JsonNode getItemInfo() {
        return JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "itemInfo.json"));
    }

}
