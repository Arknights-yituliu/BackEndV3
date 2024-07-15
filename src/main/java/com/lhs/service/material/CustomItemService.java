package com.lhs.service.material;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.Stage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CustomItemService {

    private final StageCalService stageCalService;

    private final ItemService itemService;

    private final StageService stageService;

    private final RedisTemplate<String, Object> redisTemplate;

    public CustomItemService(StageCalService stageCalService, ItemService itemService, StageService stageService, RedisTemplate<String, Object> redisTemplate) {
        this.stageCalService = stageCalService;
        this.itemService = itemService;
        this.stageService = stageService;
        this.redisTemplate = redisTemplate;
    }

    public void CustomItemValue(StageParamDTO stageParamDTO){
        List<Item> baseItemList = itemService.getBaseItemList();
        Map<String, Stage> stageGroupByStageId = stageService.getStageList(new QueryWrapper<Stage>()
                .notLike("stage_id", "tough"))
                .stream()
                .collect(Collectors.toMap(Stage::getStageId, Function.identity()));

//        for (int i = 0; i < 5; i++) {
//            itemService.ItemValueCal(baseItemList,stageParamDTO);
//            stageCalService.stageResultCal(baseItemList,stageParamDTO);
//        }
    }
}
