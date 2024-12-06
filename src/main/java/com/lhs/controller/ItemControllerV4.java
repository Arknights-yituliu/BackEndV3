package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.Stage;
import com.lhs.entity.vo.material.*;
import com.lhs.service.material.*;
import com.lhs.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "材料相关API-v4")
public class ItemControllerV4 {

    private final ItemService itemService;

    private final StageService stageService;

    private final StoreService storeService;
    private final PackInfoService packInfoService;

    private final TaskService taskService;

    private final StageResultService stageResultService;

    public ItemControllerV4(ItemService itemService, StageService stageService, StoreService storeService,
                            TaskService taskService, StageResultService stageResultService,
                            PackInfoService packInfoService) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.taskService = taskService;
        this.stageResultService = stageResultService;
        this.packInfoService = packInfoService;
    }

    @Operation(summary = "手动更新")
    @GetMapping("/stage/update")
    public Result<Map<String, List<Stage>>> updateStageResult() {
        taskService.updateStageResult();
        return Result.success();
    }


    @Operation(summary = "获取每种材料系列的关卡计算结果")
    @PostMapping("/custom/stage/result")
    public Result<Map<String, Object>> getStageResultOld(@RequestBody StageConfigDTO stageConfigDTO) {
        Map<String, Object> t3RecommendedStageV3 = stageResultService.getRecommendedStageV3(stageConfigDTO);
        return Result.success(t3RecommendedStageV3);
    }

    @Operation(summary = "获取物品价值表")
    @PostMapping("/custom/item/value")
    public Result<List<Item>> getItemValueV2(@RequestBody StageConfigDTO stageConfigDTO) {
        List<Item> items = itemService.getItemListCache(stageConfigDTO);
        return Result.success(items);
    }

    @Operation(summary = "获取搓玉推荐关卡")
    @PostMapping("/custom/stage/orundum")
    public Result<List<OrundumPerApResultVO>> getOrundumRecommendedStage(@RequestBody StageConfigDTO stageConfigDTO) {
        List<OrundumPerApResultVO> orundumPerApResultVOList = stageResultService.getOrundumRecommendedStage(stageConfigDTO);
        return Result.success(orundumPerApResultVOList);
    }

    @Operation(summary = "获取历史活动关卡")
    @PostMapping("/custom/stage/history")
    public Result<List<ActStageVO>> getHistoryActStage(@RequestBody StageConfigDTO stageConfigDTO) {
        List<ActStageVO> actStageVOList = stageResultService.getHistoryActStage(stageConfigDTO);
        return Result.success(actStageVOList);
    }

    @Operation(summary = "获取活动商店性价比")
    @PostMapping("/custom/store/act")
    public Result<List<ActivityStoreDataVO>> getStoreActData(@RequestBody StageConfigDTO stageConfigDTO) {
        List<ActivityStoreDataVO> activityStoreDataVOList = storeService.getActivityStoreData(stageConfigDTO);
        return Result.success(activityStoreDataVOList);
    }

    @Operation(summary = "获取常驻商店性价比")
    @PostMapping("/custom/store/perm")
    public Result<Map<String, List<StorePermVO>>> getStorePermData(@RequestBody StageConfigDTO stageConfigDTO) {
        Map<String, List<StorePermVO>> storePermMap = storeService.getStorePermMap(stageConfigDTO);
        return Result.success(storePermMap);
    }

    @Operation(summary = "获取礼包商店性价比")
    @PostMapping("/custom/store/pack")
    public Result<List<PackInfoVO>> getStorePackData(@RequestBody StageConfigDTO stageConfigDTO) {
        List<PackInfoVO> packInfoVOS = packInfoService.listPackInfo(stageConfigDTO);
        return Result.success(packInfoVOS);
    }


}
