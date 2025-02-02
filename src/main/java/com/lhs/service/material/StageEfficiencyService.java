package com.lhs.service.material;

import com.lhs.common.util.IdGenerator;
import com.lhs.common.util.PenguinMatrixCollect;
import com.lhs.entity.dto.material.PenguinMatrixDTO;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.Stage;
import com.lhs.entity.po.material.StageEfficiency;
import com.lhs.mapper.material.QuantileMapper;
import com.lhs.mapper.material.StageResultDetailMapper;
import com.lhs.mapper.material.StageResultMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StageEfficiencyService {

    private final StageService stageService;
    private final QuantileMapper quantileMapper;
    private final ItemService itemService;
    private final StageResultMapper stageResultMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final StageResultDetailMapper stageResultDetailMapper;
    private final IdGenerator idGenerator;

    public StageEfficiencyService(StageService stageService,
                                  QuantileMapper quantileMapper,
                                  ItemService itemService,
                                  StageResultMapper stageResultMapper,
                                  RedisTemplate<String, Object> redisTemplate,
                                  StageResultDetailMapper stageResultDetailMapper) {
        this.stageService = stageService;
        this.quantileMapper = quantileMapper;
        this.itemService = itemService;
        this.stageResultMapper = stageResultMapper;
        this.redisTemplate = redisTemplate;
        this.stageResultDetailMapper = stageResultDetailMapper;
        this.idGenerator = new IdGenerator(1L);
    }


    public void stageEfficiencyCalc(StageConfigDTO stageConfigDTO) {
        Map<String, Item> itemMapCache = itemService.getItemMapCache(stageConfigDTO);
        Map<String, Stage> stageInfoMap = stageService.getStageInfoMap();
        Map<String, String> stageBlackMap = stageConfigDTO.getStageBlackMap();
        Map<String, List<PenguinMatrixDTO>> penguinMatrix = PenguinMatrixCollect
                .filterAndMergePenguinData(itemMapCache, stageInfoMap,
                        stageBlackMap, stageConfigDTO.getSampleSize());

        for(String stageId : penguinMatrix.keySet()){
            List<PenguinMatrixDTO> stageDropList = penguinMatrix.get(stageId);
            Stage stageInfo = stageInfoMap.get(stageId);
            Integer apCost =stageInfo.getApCost();
            String stageCode = stageInfo.getStageCode();
            String zoneName = stageInfo.getZoneName();
            String zoneId = stageInfo.getZoneId();
            String stageType = stageInfo.getStageType();

            double efficiency = 0.0;
            double dropValueCount = 0.0;

            StageEfficiency stageEfficiency = new StageEfficiency();
            for(PenguinMatrixDTO stageDrop :stageDropList){
                String itemId = stageDrop.getItemId();
                Integer quantity = stageDrop.getQuantity();
                Integer times = stageDrop.getTimes();

            }
        }

    }
}
