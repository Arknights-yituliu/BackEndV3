package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.material.Item;
import com.lhs.entity.po.material.Stage;
import com.lhs.entity.po.material.StorePerm;
import com.lhs.entity.vo.material.*;
import com.lhs.service.material.*;
import com.lhs.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "材料相关API-v3")
public class ItemControllerV3 {

    private final ItemService itemService;

    private final StageService stageService;

    private final StoreService storeService;
    private final PackInfoService packInfoService;

    private final TaskService taskService;

    private final StageResultService stageResultService;

    public ItemControllerV3(ItemService itemService, StageService stageService, StoreService storeService,
                            TaskService taskService, StageResultService stageResultService,
                            PackInfoService packInfoService) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.taskService = taskService;
        this.stageResultService = stageResultService;
        this.packInfoService = packInfoService;
    }




    @Operation(summary = "获取每种材料系列的关卡计算结果")
    @GetMapping("/stage/result")
    public Result<Map<String, Object>> getStageResultOld(@RequestParam(required = false, defaultValue = "0.633") Double expCoefficient,
                                                      @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);
        Map<String, Object> t3RecommendedStageV3 = stageResultService.getT3RecommendedStageV3(stageConfigDTO);
        return Result.success(t3RecommendedStageV3);
    }



    @Operation(summary = "获取搓玉推荐关卡")
    @GetMapping("/stage/orundum")
    public Result<List<OrundumPerApResultVO>> getStageResultOrundum(@RequestParam(required = false, defaultValue = "0.633") Double expCoefficient,
                                                                    @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setExpCoefficient(expCoefficient);
        stageConfigDTO.setSampleSize(sampleSize);
        List<OrundumPerApResultVO> orundumPerApResultVOList = stageResultService.getOrundumRecommendedStage(stageConfigDTO);
        return Result.success(orundumPerApResultVOList);
    }

    @Operation(summary = "获取历史活动关卡")
    @GetMapping("/stage/act")
    public Result<List<ActStageVO>> getStageResultClosedActivities(@RequestParam(required = false, defaultValue = "0.633") Double expCoefficient,
                                                                   @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setExpCoefficient(expCoefficient);
        stageConfigDTO.setSampleSize(sampleSize);
        List<ActStageVO> actStageVOList = stageResultService.getHistoryActStage(stageConfigDTO);
        return Result.success(actStageVOList);
    }

    @Operation(summary = "获取关卡信息")
    @GetMapping("/stage/info")
    public Result<Object> getStageInfo(){
        return Result.success(stageService.getStageInfo());
    }


    @Operation(summary = "查询新章的关卡效率")
    @GetMapping("/stage/chapter")
    public Result<List<StageResultVOV2>> getStageResultByZone(@RequestParam(required = false, defaultValue = "0.633") Double expCoefficient,
                                                              @RequestParam(required = false, defaultValue = "300") Integer sampleSize,
                                                              @RequestParam String zoneCode) {

        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setExpCoefficient(expCoefficient);
        stageConfigDTO.setSampleSize(sampleSize);

        List<StageResultVOV2> stageByZoneName = stageResultService.getStageByZoneName(stageConfigDTO, "main_" + zoneCode);
        return Result.success(stageByZoneName);
    }

    @Operation(summary = "获取关卡目录表")
    @GetMapping("/stage/zone")
    public Result<Map<String, List<ZoneTableVO>>> getStageMenu() {
        Map<String, List<ZoneTableVO>> zoneTable = stageService.getZoneTable();
        return Result.success(zoneTable);
    }

    @Operation(summary = "查询关卡详细信息")
    @GetMapping("/stage/detail")
    public Result<Map<String, StageResultDetailVO>> getStageResultDetail(@RequestParam(required = false, defaultValue = "0.633") Double expCoefficient,
                                                                         @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setExpCoefficient(expCoefficient);
        stageConfigDTO.setSampleSize(sampleSize);
        Map<String, StageResultDetailVO> allStageResult = stageResultService.getAllStageResult(stageConfigDTO);
        return Result.success(allStageResult);
    }

    @Operation(summary = "获取物品价值表")
    @GetMapping("/item/value")
    public Result<List<Item>> getItemValueV2(@RequestParam(required = false, defaultValue = "0.633") Double expCoefficient,
                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setExpCoefficient(expCoefficient);
        stageConfigDTO.setSampleSize(sampleSize);
        List<Item> items = itemService.getItemListCache(stageConfigDTO);
        return Result.success(items);
    }

    @Operation(summary = "获取常驻商店性价比")
    @GetMapping("/store/perm")
    public Result<Map<String, List<StorePerm>>> getStorePermData() {
        Map<String, List<StorePerm>> storePerm = storeService.getStorePerm();
        return Result.success(storePerm);
    }

    //    @TakeCount(name = "活动商店性价比")
//    @Operation(summary = "获取活动商店性价比")
//    @GetMapping("/store/act")
//    public Result<List<ActivityStoreDataVO>> getStoreActData() {
//
//        List<ActivityStoreDataVO> activityStoreDataVOList = storeService.getActivityStoreData();
//        return Result.success(activityStoreDataVOList);
//    }

    @Operation(summary = "获取礼包商店性价比")
    @GetMapping("/store/pack")
    public Result<List<PackInfoVO>> listPackInfo(@RequestParam(required = false, defaultValue = "0.633") Double expCoefficient,
                                                 @RequestParam(required = false, defaultValue = "300") Integer sampleSize){
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setExpCoefficient(expCoefficient);
        stageConfigDTO.setSampleSize(sampleSize);
        List<PackInfoVO> list =  packInfoService.listPackInfo(stageConfigDTO);
        return Result.success(list);
    }

    @Operation(summary = "获取礼包商店性价比")
    @GetMapping("/store/pack/tag")
    public Result<String> getPackInfoTag(){
        String tag = packInfoService.packInfoTag();
        return Result.success(tag);
    }

}
