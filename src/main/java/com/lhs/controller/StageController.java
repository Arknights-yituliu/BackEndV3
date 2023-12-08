package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.item.StageParamDTO;
import com.lhs.entity.po.dev.HoneyCake;
import com.lhs.entity.po.item.Item;
import com.lhs.entity.po.item.Stage;
import com.lhs.entity.po.item.StorePerm;
import com.lhs.entity.vo.item.*;
import com.lhs.service.item.*;
import com.lhs.task.ItemTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "材料相关API-v2")
@CrossOrigin()
public class StageController {

    private final ItemService itemService;
    private final StageService stageService;
    private final StoreService storeService;
    private final StageResultService stageResultService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ItemTask itemTask;


    public StageController(ItemService itemService, StageService stageService, StoreService storeService, StageResultService stageResultService, RedisTemplate<String, Object> redisTemplate, ItemTask itemTask) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.stageResultService = stageResultService;
        this.redisTemplate = redisTemplate;
        this.itemTask = itemTask;
    }

    @Operation(summary = "手动更新")
    @GetMapping("/stage/update")
    public Result<Map<String, List<Stage>>> updateStageResult() {
        itemTask.updateStageResult();
        return Result.success();
    }



    //    @TakeCount(name = "蓝材料推荐关卡")
    @Operation(summary = "获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3/v2")
    public Result<Map<String, Object>> getStageResultT3(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setSampleSize(sampleSize);
        stageParamDTO.setExpCoefficient(expCoefficient);
        Map<String, Object> hashMap = stageResultService.getT3RecommendedStageV2(stageParamDTO.getVersion());
        return Result.success(hashMap);
    }

    @Operation(summary = "获取每种材料系列的关卡计算结果")
    @GetMapping("/stage/V3")
    public Result<Map<String, Object>> getStageResultT3V3(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                      @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setSampleSize(sampleSize);
        stageParamDTO.setExpCoefficient(expCoefficient);
        Map<String, Object> t3RecommendedStageV3 = stageResultService.getT3RecommendedStageV3(stageParamDTO.getVersion());

        return Result.success(t3RecommendedStageV3);
    }

    @Operation(summary = "自定义材料价值计算")
    @GetMapping("/item/custom")
    public Result<Map<String, Object>> customStageResult(@RequestBody StageParamDTO stageParamDTO) {

        Map<String, Object> t3RecommendedStageV3 = stageResultService.getT3RecommendedStageV3(stageParamDTO.getVersion());

        return Result.success(t3RecommendedStageV3);
    }

    @Operation(summary = "获取每种材料系列的关卡计算结果")
    @GetMapping("/stage/result")
    public Result<Map<String, Object>> getStageResult(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setSampleSize(sampleSize);
        stageParamDTO.setExpCoefficient(expCoefficient);
        Map<String, Object> t3RecommendedStageV3 = stageResultService.getT3RecommendedStageV3(stageParamDTO.getVersion());

        return Result.success(t3RecommendedStageV3);
    }

    //    @TakeCount(name = "绿材料推荐关卡")
    @Operation(summary = "获取绿材料推荐关卡按期望倒序")
    @GetMapping("/stage/t2/v2")
    public Result<List<RecommendedStageVO>> getStageResultT2(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setSampleSize(sampleSize);
        stageParamDTO.setExpCoefficient(expCoefficient);
        List<RecommendedStageVO> recommendedStageVOList = stageResultService.getT2RecommendedStage(stageParamDTO.getVersion());

        return Result.success(recommendedStageVOList);
    }

    @Operation(summary = "获取搓玉推荐关卡")
    @GetMapping("/stage/orundum")
    public Result<List<OrundumPerApResultVO>> getStageResult_Orundum(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                                     @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        stageParamDTO.setSampleSize(sampleSize);
        List<OrundumPerApResultVO> orundumPerApResultVOList = stageResultService.getOrundumRecommendedStage(stageParamDTO.getVersion());
        return Result.success(orundumPerApResultVOList);
    }

    @Operation(summary = "获取历史活动关卡")
    @GetMapping("/stage/act")
    public Result<List<ActStageVO>> getStageResultClosedActivities(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                                   @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        stageParamDTO.setSampleSize(sampleSize);
        List<ActStageVO> actStageVOList = stageResultService.getActStage(stageParamDTO.getVersion());


        return Result.success(actStageVOList);
    }

    @Operation(summary = "查询新章的关卡效率")
    @GetMapping("/stage/chapter")
    public Result<List<StageResultVOV2>> getStageResultByZone(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                              @RequestParam(required = false, defaultValue = "300") Integer sampleSize,
                                                              @RequestParam String zone) {

        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        stageParamDTO.setSampleSize(sampleSize);

        List<StageResultVOV2> stageByZoneName = stageResultService.getStageByZoneName(stageParamDTO, "main_" + zone);
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
    public Result<Map<String, StageResultDetailVO>> getStageResultDetail(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                                               @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        stageParamDTO.setSampleSize(sampleSize);
        Map<String, StageResultDetailVO> allStageResult = stageResultService.getAllStageResult(stageParamDTO.getVersion());
        return Result.success(allStageResult);
    }



    @Operation(summary = "获取物品价值表")
    @GetMapping("/item/value")
    public Result<List<Item>> getItemValueV2(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setExpCoefficient(expCoefficient);
        stageParamDTO.setSampleSize(sampleSize);
        List<Item> items = itemService.getItemListCache(stageParamDTO.getVersion());
        return Result.success(items);
    }

    @Operation(summary = "获取常驻商店性价比")
    @GetMapping("/store/perm")
    public Result<Map<String, List<StorePerm>>> getStorePermData() {
        Map<String, List<StorePerm>> storePerm = storeService.getStorePerm();
        return Result.success(storePerm);
    }

    //    @TakeCount(name = "活动商店性价比")
    @Operation(summary = "获取活动商店性价比")
    @GetMapping("/store/act")
    public Result<List<StoreActVO>> getStoreActData() {
        List<StoreActVO> storeActVOList = storeService.getStoreAct();
        return Result.success(storeActVOList);
    }

    @Operation(summary = "攒抽计算器活动排期")
    @GetMapping("/store/honeyCake")
    public Result<Map<String, HoneyCake>> getHoneyCake() {
        Map<String, HoneyCake> honeyCake = storeService.getHoneyCake();
        return Result.success(honeyCake);
    }

    @Operation(summary = "攒抽计算器活动排期")
    @GetMapping("/store/honeyCake/list")
    public Result<List<HoneyCake>> getHoneyCakeList() {
        List<HoneyCake> honeyCakeList = storeService.getHoneyCakeList();
        return Result.success(honeyCakeList);
    }

    @Operation(summary = "材料表导出（Excel格式）")
    @GetMapping("/item/export/excel")
    public void exportItemExcel(HttpServletResponse response) {
        itemService.exportItemExcel(response);
    }

    @Operation(summary = "材料表导出（Json格式）")
    @GetMapping("/item/export/json")
    public void exportItemJson(HttpServletResponse response) {
        itemService.exportItemJson(response);
    }

    @GetMapping("/stage/resetCache")
    public Result<List<String>> authResetCache() {
        List<String> message = stageResultService.resetCache();
        return Result.success(message);
    }





    //    @TakeCount(name = "蓝材料推荐关卡")
    @Operation(summary = "获取蓝材料推荐关卡按效率倒序v1")
    @GetMapping("/stage/t3")
    public Result<List<List<StageResultVO>>> getStageResultT3V1(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                              @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setSampleSize(sampleSize);
        stageParamDTO.setExpCoefficient(expCoefficient);
        return Result.success(stageResultService.getT3RecommendedStageV1(stageParamDTO.getVersion()));
    }

    //    @TakeCount(name = "绿材料推荐关卡")
    @Operation(summary = "获取绿材料推荐关卡按期望倒序v1")
    @GetMapping("/stage/t2")
    public Result<List<List<StageResultVO>>> getStageResultT2V1(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                              @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setSampleSize(sampleSize);
        stageParamDTO.setExpCoefficient(expCoefficient);

        return Result.success(stageResultService.getT2RecommendedStageV1(stageParamDTO.getVersion()));
    }

    @Operation(summary = "获取历史活动关卡v1")
    @GetMapping("/stage/closed")
    public Result<List<ActStageVO>> getStageResultClosedV1(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                                @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageParamDTO stageParamDTO = new StageParamDTO();
        stageParamDTO.setSampleSize(sampleSize);
        stageParamDTO.setExpCoefficient(expCoefficient);

        return Result.success(stageResultService.getActStage(stageParamDTO.getVersion()));
    }

}
