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
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.po.material.Stage;
import com.lhs.service.material.CustomItemService;
import com.lhs.service.material.StageService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomItemServiceImpl implements CustomItemService {

    private final StageService stageService;

    private static final Integer BASE_LMD_VALUE = 36/10000;
    private static final Integer BASE_EXP_VALUE = 36/10000;

    public CustomItemServiceImpl(StageService stageService) {
        this.stageService = stageService;
    }

    @Override
    public void customItemValueCalculation() {

    }


    @Override
    public void getCustomItemList() {
        JsonNode recruitmentTableJson = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "recruitment_table.json"));

        //招募许可定价方案
        JsonNode recruitmentPermitPricing = recruitmentTableJson.get("recruitmentPermitPricing");
        //公开招募干员概率
        JsonNode operatorRecruitmentRates = recruitmentTableJson.get("operatorRecruitmentRates");

        String itemInfoText = FileUtil.read(ConfigUtil.DataFilePath + "item_info.json");

        List<ItemInfoDTO> itemInfoDTOList = JsonMapper.parseObject(itemInfoText, new TypeReference<>() {
        });

        Map<Integer, Map<String,Double>> workshopByproductWeightMap = new HashMap<>();

        for(ItemInfoDTO itemInfoDTO : itemInfoDTOList){
            if(itemInfoDTO.getWeight()>0){
               if(!workshopByproductWeightMap.containsKey(itemInfoDTO.getRarity())){
                  workshopByproductWeightMap.put(itemInfoDTO.getRarity(),new HashMap<>());
               }
               workshopByproductWeightMap.get(itemInfoDTO.getRarity()).put(itemInfoDTO.getItemId(), itemInfoDTO.getWeight());
            }
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


        Map<String, Integer> stageBlacklistMap = itemValueConfigDTO.getStageBlacklist().stream().collect(Collectors.toMap(stageId->stageId, stageId->1));

        int sampleSize = 300;
        if (itemValueConfigDTO.getSampleSize() != null) {
            sampleSize = itemValueConfigDTO.getSampleSize();
        }
    }

    private void checkItemValueConfig(ItemValueConfigDTO configDTO){
        if(configDTO.getSampleSize()==null&&configDTO.getSampleSize()<=50){
            throw new ServiceException(ResultCode.PARAM_IS_INVALID);
        }


    }

    @RedisCacheable(key = "Json:Recruitment_Table")
    private JsonNode getRecruitmentTable(){
        return JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "recruitment_table.json"));
    }

    @RedisCacheable(key = "Json:Item_Info")
    private JsonNode getItemInfo(){
        return JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "itemInfo.json"));
    }

}
