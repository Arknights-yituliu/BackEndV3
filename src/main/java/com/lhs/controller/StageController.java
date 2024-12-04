package com.lhs.controller;

import com.lhs.common.util.Result;
import com.lhs.entity.dto.material.StageConfigDTO;
import com.lhs.entity.po.admin.HoneyCake;
import com.lhs.entity.vo.material.*;
import com.lhs.service.material.*;
import com.lhs.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "材料相关API-v2")
public class StageController {

    private final ItemService itemService;
    private final StageService stageService;
    private final StoreService storeService;
    private final StageResultService stageResultService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final TaskService taskService;


    public StageController(ItemService itemService, StageService stageService, StoreService storeService, StageResultService stageResultService, RedisTemplate<String, Object> redisTemplate, TaskService taskService) {
        this.itemService = itemService;
        this.stageService = stageService;
        this.storeService = storeService;
        this.stageResultService = stageResultService;
        this.redisTemplate = redisTemplate;
        this.taskService = taskService;
    }





    //    @TakeCount(name = "蓝材料推荐关卡")
    @Operation(summary = "获取蓝材料推荐关卡按效率倒序")
    @GetMapping("/stage/t3/v2")
    public Result<Map<String, Object>> getStageResultT3(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);
        Map<String, Object> hashMap = stageResultService.getT3RecommendedStageV2(stageConfigDTO.getVersion());
        return Result.success(hashMap);
    }



    @Operation(summary = "自定义材料价值计算")
    @GetMapping("/item/custom")
    public Result<Map<String, Object>> customStageResult(@RequestBody StageConfigDTO stageConfigDTO) {

        Map<String, Object> t3RecommendedStageV3 = stageResultService.getT3RecommendedStageV3(stageConfigDTO.getVersion());

        return Result.success(t3RecommendedStageV3);
    }



    //    @TakeCount(name = "绿材料推荐关卡")
    @Operation(summary = "获取绿材料推荐关卡按期望倒序")
    @GetMapping("/stage/t2/v2")
    public Result<List<RecommendedStageVO>> getStageResultT2(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                             @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);
        List<RecommendedStageVO> recommendedStageVOList = stageResultService.getT2RecommendedStage(stageConfigDTO.getVersion());

        return Result.success(recommendedStageVOList);
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
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);
        return Result.success(stageResultService.getT3RecommendedStageV1(stageConfigDTO.getVersion()));
    }

    //    @TakeCount(name = "绿材料推荐关卡")
    @Operation(summary = "获取绿材料推荐关卡按期望倒序v1")
    @GetMapping("/stage/t2")
    public Result<List<List<StageResultVO>>> getStageResultT2V1(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                              @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);

        return Result.success(stageResultService.getT2RecommendedStageV1(stageConfigDTO.getVersion()));
    }

    @Operation(summary = "获取历史活动关卡v1")
    @GetMapping("/stage/closed")
    public Result<List<ActStageVO>> getStageResultClosedV1(@RequestParam(required = false, defaultValue = "0.625") Double expCoefficient,
                                                                @RequestParam(required = false, defaultValue = "300") Integer sampleSize) {
        StageConfigDTO stageConfigDTO = new StageConfigDTO();
        stageConfigDTO.setSampleSize(sampleSize);
        stageConfigDTO.setExpCoefficient(expCoefficient);

        return Result.success(stageResultService.getHistoryActStage(stageConfigDTO.getVersion()));
    }

}
