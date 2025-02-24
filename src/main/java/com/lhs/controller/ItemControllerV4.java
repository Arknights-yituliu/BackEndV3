package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.QueryStageDropDTO;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.Stage;
import com.lhs.entity.vo.material.*;
import com.lhs.service.material.*;
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
    private final StageCalService stageCalService;
    private final StageResultService stageResultService;
    private final StageDropService stageDropService;

    public ItemControllerV4(ItemService itemService, StageService stageService, StoreService storeService,
                            StageResultService stageResultService,
                            PackInfoService packInfoService, StageCalService stageCalService, StageDropService stageDropService) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.stageResultService = stageResultService;
        this.packInfoService = packInfoService;
        this.stageCalService = stageCalService;
        this.stageDropService = stageDropService;
    }

    @Operation(summary = "手动更新")
    @GetMapping("/update/stage/result")
    public Result<Map<String, List<Stage>>> updateStageResult() {
        stageCalService.updateStageResultByTaskConfig();
        return Result.success();
    }

    @Operation(summary = "关卡掉落")
    @PostMapping("/stage/drop")
    public Result<List<StageDropStatisticsVO>> getStageDropByStageId(@RequestBody QueryStageDropDTO queryStageDropDTO) {

        return Result.success(stageDropService.getStageDropByStageId(queryStageDropDTO));
    }

    @Operation(summary = "检查当前材料价值表是否需要更新")
    @GetMapping("/check/item/value")
    public Result<Long> checkItemValue(@RequestBody StageConfigDTO stageConfigDTO) {
        Long updateTime =  itemService.checkItemValue(stageConfigDTO);
        return Result.success();
    }

    @Operation(summary = "获取每种材料系列的关卡计算结果")
    @PostMapping("/custom/stage/result")
    public Result<Map<String, Object>> getStageResultOld(@RequestBody StageConfigDTO stageConfigDTO) {
        Map<String, Object> t3RecommendedStageV3 = stageResultService.getRecommendedStage(stageConfigDTO);
        return Result.success(t3RecommendedStageV3);
    }

    @Operation(summary = "获取物品价值表")
    @PostMapping("/custom/item/value")
    public Result<List<Item>> getItemValue(@RequestBody StageConfigDTO stageConfigDTO) {
        List<Item> items = itemService.getItemListCache(stageConfigDTO);
        return Result.success(items);
    }

    @Operation(summary = "获取物品价值表")
    @PostMapping("/item/value/v5")
    public Result<List<Item>> getItemValue1(@RequestBody StageConfigDTO stageConfigDTO) {
        List<Item> items = itemService.getCustomItemList(stageConfigDTO);
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



    @Operation(summary = "获取礼包商店性价比")
    @PostMapping("/custom/store/pack")
    public Result<List<PackInfoVO>> getStorePackData(@RequestBody StageConfigDTO stageConfigDTO) {
        List<PackInfoVO> packInfoVOS = packInfoService.listPackInfo(stageConfigDTO);
        return Result.success(packInfoVOS);
    }

    @Operation(summary = "获取关卡信息")
    @GetMapping("/stage/info")
    public Result< List<Map<String, Object>>> getStageInfo() {
        List<Map<String, Object>> stageInfo = stageService.getStageInfo();
        return Result.success(stageInfo);
    }
}
