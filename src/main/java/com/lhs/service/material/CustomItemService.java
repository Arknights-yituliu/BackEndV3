package com.lhs.service.material;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.entity.dto.item.custom.RecruitmentPermitPrice;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomItemService {


    private static final Integer BASE_LMD_VALUE = 36/10000;
    private static final Integer BASE_EXP_VALUE = 36/10000;

    public void customItemValueCalculation() {

        JsonNode recruitmentInfoJson = JsonMapper.parseJSONObject(FileUtil.read(ConfigUtil.DataFilePath + "recruitment_info.json"));

        //招募许可定价方案
        JsonNode recruitmentPermitPricing = recruitmentInfoJson.get("recruitmentPermitPricing");
        //公开招募干员概率
        JsonNode operatorRecruitmentRates = recruitmentInfoJson.get("operatorRecruitmentRates");





    }


}
