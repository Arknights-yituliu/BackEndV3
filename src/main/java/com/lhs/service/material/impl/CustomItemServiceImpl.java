package com.lhs.service.material.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lhs.common.config.ConfigUtil;
import com.lhs.common.util.FileUtil;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.PenguinMatrixCollect;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.ItemInfo;
import com.lhs.entity.po.material.Stage;
import com.lhs.service.material.ItemService;
import com.lhs.service.material.StageCalService;
import com.lhs.service.material.StageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomItemServiceImpl {

    private final StageCalService stageCalService;

    private final ItemService itemService;

    private final StageService stageService;

    private final RedisTemplate<String, Object> redisTemplate;

    public CustomItemServiceImpl(StageCalService stageCalService, ItemService itemService, StageService stageService, RedisTemplate<String, Object> redisTemplate) {
        this.stageCalService = stageCalService;
        this.itemService = itemService;
        this.stageService = stageService;
        this.redisTemplate = redisTemplate;
    }

    private void getItemValueCorrectionTerm(StageConfigDTO stageConfigDTO) {
        Map<String, Stage> stageInfoMap = stageService.getStageMapKeyIsStageId();
        String read = FileUtil.read(ConfigUtil.DataFilePath + "item_info.json");
        List<ItemInfo> itemInfoList= JsonMapper.parseObject(read, new TypeReference<>() {
        });
//        JsonMapper.parseJSONObject(List< ItemInfo > )
//        PenguinMatrixCollect.getStageInfoAndDropMap(stageInfoMap, stageConfigDTO)
    }

    public void calculatedItemValue(StageConfigDTO stageConfigDTO) {


    }

}
